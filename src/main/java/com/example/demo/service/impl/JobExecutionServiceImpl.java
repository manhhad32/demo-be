package com.example.demo.service.impl;

import static com.example.demo.constant.MessageCode.MESSAGE_JOB_PROCESSING_FAIL;

import com.example.demo.constant.JobStatus;
import com.example.demo.entity.Job;
import com.example.demo.entity.JobExecutionLog;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.JobExecutionLogRepository;
import com.example.demo.repository.JobRepository;
import com.example.demo.service.JobExecutionService;
import com.example.demo.utils.MessageUtil;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class JobExecutionServiceImpl implements JobExecutionService {

  private final JobRepository jobRepository;
  private final JobExecutionLogRepository logRepository;
  private final MessageUtil messageUtil;

  @Autowired
  public JobExecutionServiceImpl(JobRepository jobRepository,
      JobExecutionLogRepository logRepository, MessageUtil messageUtil) {
    this.jobRepository = jobRepository;
    this.logRepository = logRepository;
    this.messageUtil = messageUtil;
  }

  @Retryable(
      retryFor = {RuntimeException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000, multiplier = 2)
  )
  @Transactional
  @Override
  public void process(Integer jobId) {
    Job job = jobRepository.findById(jobId).orElseThrow();
    log.info("Executing job id: {}", jobId);

    // Simulated Processing Logic
    if (job.getPayload() != null && Boolean.TRUE.equals(job.getPayload().get("fail"))) {
      log.error("Job id {} failed due to simulated 'fail: true' in payload.", jobId);
      throw new CustomException(MESSAGE_JOB_PROCESSING_FAIL.getCode(),
          messageUtil.getMessage(MESSAGE_JOB_PROCESSING_FAIL));
    }

    if ("EMAIL".equals(job.getTypeJob().getType())) {
      log.info("Sending email for job {}...", jobId);
    }

    job.setStatus(JobStatus.COMPLETED);
    jobRepository.save(job);

    JobExecutionLog executionLog = JobExecutionLog.builder()
        .job(job)
        .status(JobStatus.COMPLETED)
        .build();
    logRepository.save(executionLog);
  }

  @Recover
  @Transactional
  public void recover(RuntimeException e, Integer jobId) {
    Job job = jobRepository.findById(jobId).orElseThrow();
    log.error("Job id {} failed after all retries: {}", jobId, e.getMessage());

    long retryCount = logRepository.countByJobIdAndStatus(jobId, JobStatus.FAILED) + 1;

    if (retryCount >= 3) {
      job.setStatus(JobStatus.FAILED);
      job.setNextRunAt(null);
    } else {
      job.setStatus(JobStatus.PENDING);
      job.setNextRunAt(LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(5L * retryCount));
    }
    jobRepository.save(job);

    JobExecutionLog executionLog = JobExecutionLog.builder()
        .job(job)
        .status(JobStatus.FAILED)
        .errorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())
        .build();
    logRepository.save(executionLog);
  }
}
