package com.example.demo.service.impl;

import static com.example.demo.constant.MessageCode.MESSAGE_ERROR_INPUT_ERROR;

import com.example.demo.constant.MessageCode;
import com.example.demo.constant.JobStatus;
import com.example.demo.dto.request.CreateJobRequestDto;
import com.example.demo.dto.response.JobResponseDto;
import com.example.demo.entity.Job;
import com.example.demo.entity.TypeJob;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.JobRepository;
import com.example.demo.repository.TypeJobRepository;
import com.example.demo.service.JobService;
import com.example.demo.utils.MessageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class JobServiceImpl implements JobService {
  private final JobRepository jobRepository;
  private final TypeJobRepository typeJobRepository;
  private final MessageUtil messageUtil;

  @Autowired
  public JobServiceImpl(JobRepository jobRepository, TypeJobRepository typeJobRepository, MessageUtil messageUtil) {
    this.jobRepository = jobRepository;
    this.typeJobRepository = typeJobRepository;
    this.messageUtil = messageUtil;
  }

  @Transactional()
  @Override
  public JobResponseDto createJob(CreateJobRequestDto requestDto) {
    if(requestDto == null ) {
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
}
