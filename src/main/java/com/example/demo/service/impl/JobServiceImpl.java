package com.example.demo.service.impl;

import static com.example.demo.constant.MessageCode.MESSAGE_ERROR_INPUT_ERROR;

import com.example.demo.constant.JobStatus;
import com.example.demo.constant.MessageCode;
import com.example.demo.dto.request.CreateJobRequestDto;
import com.example.demo.dto.response.JobDetailPage;
import com.example.demo.dto.response.JobDetailResponseDto;
import com.example.demo.dto.response.JobResponseDto;
import com.example.demo.entity.Job;
import com.example.demo.entity.TypeJob;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.JobExecutionLogRepository;
import com.example.demo.repository.JobRepository;
import com.example.demo.repository.TypeJobRepository;
import com.example.demo.service.JobExecutionService;
import com.example.demo.service.JobService;
import com.example.demo.utils.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
public class JobServiceImpl implements JobService {

  private final JobRepository jobRepository;
  private final TypeJobRepository typeJobRepository;
  private final MessageUtil messageUtil;
  private final JobExecutionService jobExecutionService;
  private final TransactionTemplate transactionTemplate;
  private final JobExecutionLogRepository jobExecutionLogRepository;

  @Autowired
  public JobServiceImpl(JobRepository jobRepository, TypeJobRepository typeJobRepository,
      MessageUtil messageUtil, JobExecutionService jobExecutionService,
      TransactionTemplate transactionTemplate,
      JobExecutionLogRepository jobExecutionLogRepository) {
    this.jobRepository = jobRepository;
    this.typeJobRepository = typeJobRepository;
    this.messageUtil = messageUtil;
    this.jobExecutionService = jobExecutionService;
    this.transactionTemplate = transactionTemplate;
    this.jobExecutionLogRepository = jobExecutionLogRepository;
  }

  @Transactional()
  @Override
  public JobResponseDto createJob(CreateJobRequestDto requestDto) {
    if (requestDto == null) {
      throw new CustomException(MESSAGE_ERROR_INPUT_ERROR.getCode(),
          messageUtil.getMessage(MESSAGE_ERROR_INPUT_ERROR));
    }
    // and validate other fields.
    TypeJob typeJob = typeJobRepository.findByType(requestDto.getType());
    if (typeJob == null) {
      String msgError = String.format(messageUtil.getMessage(MessageCode.MESSAGE_TYPE_JOB_NOT_FOUND)
          , requestDto.getType());
      throw new CustomException(MESSAGE_ERROR_INPUT_ERROR.getCode(), msgError);
    }
    Job job = Job.builder()
        .typeJob(typeJob)
        .payload(requestDto.getPayload())
        .status(JobStatus.PENDING)
        .build();
    Job savedJob = jobRepository.save(job);
    return JobResponseDto.builder()
        .id(savedJob.getId())
        .status(savedJob.getStatus().name())
        .build();
  }

  @Transactional(readOnly = true)
  @Override
  public JobDetailResponseDto getJobById(Integer id) {
    Job job = jobRepository.findById(id).orElseThrow(() ->
        new CustomException(MessageCode.MESSAGE_NOT_FOUND.getCode(),
            messageUtil.getMessage(MessageCode.MESSAGE_NOT_FOUND)));
    return mapToJobDetailResponseDto(job);
  }

  @Transactional(readOnly = true)
  @Override
  public JobDetailPage getJobs(String status, Pageable pageable) {
    Page<Job> jobs;
    if (status != null && !status.trim().isEmpty()) {
      try {
        JobStatus jobStatus = JobStatus.valueOf(status.toUpperCase());
        jobs = jobRepository.findByStatus(jobStatus, pageable);
      } catch (IllegalArgumentException e) {
        throw new CustomException(MessageCode.MESSAGE_ERROR_INPUT_ERROR.getCode(),
            "Invalid status: " + status);
      }
    } else {
      jobs = jobRepository.findAll(pageable);
    }
    Page<JobDetailResponseDto> dtoPage = jobs.map(this::mapToJobDetailResponseDto);
    return JobDetailPage.builder()
        .jobDetailResponseDtos(dtoPage.getContent())
        .page(dtoPage.getNumber())
        .sizePage(dtoPage.getSize())
        .totalRows(dtoPage.getTotalElements())
        .totalPage(dtoPage.getTotalPages())
        .build();
  }

  @Override
  public List<JobDetailResponseDto> processPendingJobs() {
    // 1. Fetch and lock jobs in a single transaction
    List<Job> lockedJobs = transactionTemplate.execute(status -> {
      List<Job> jobs = jobRepository.findPendingJobsForProcessing(10);
      if (jobs != null && !jobs.isEmpty()) {
        for (Job job : jobs) {
          job.setStatus(JobStatus.PROCESSING);
        }
        return jobRepository.saveAll(jobs);
      }
      log.info("No pending jobs to process");
      return new ArrayList<>();
    });

    if (lockedJobs == null || lockedJobs.isEmpty()) {
      return new ArrayList<>();
    }

    log.info("Found {} pending jobs to process", lockedJobs.size());
    List<JobDetailResponseDto> processedJobDetails = new ArrayList<>();

    // 2. Process each job individually (Spring Retry handles inner transactions)
    for (Job job : lockedJobs) {
      try {
        jobExecutionService.process(job.getId());
      } catch (Exception e) {
        log.error("Fatal error processing job id: {}", job.getId(), e);
      }
      // Re-fetch to get updated status/retry count after execution.
      // We must wrap this in transactionTemplate because getJobById is called internally 
      // (self-invocation) which bypasses its @Transactional proxy.
      JobDetailResponseDto dto = transactionTemplate.execute(status -> {
        Job updatedJob = jobRepository.findById(job.getId()).orElse(job);
        return mapToJobDetailResponseDto(updatedJob);
      });
      processedJobDetails.add(dto);
    }
    return processedJobDetails;
  }

  private JobDetailResponseDto mapToJobDetailResponseDto(Job job) {
    return JobDetailResponseDto.builder()
        .id(job.getId())
        .type(job.getTypeJob() != null ? job.getTypeJob().getType() : null)
        .payload(job.getPayload())
        .status(job.getStatus() != null ? job.getStatus().name() : null)
        .retryCount(
            (int) jobExecutionLogRepository.countByJobIdAndStatus(job.getId(), JobStatus.FAILED))
        .createdAt(job.getCreatedAt())
        .updatedAt(job.getUpdatedAt())
        .build();
  }
}
