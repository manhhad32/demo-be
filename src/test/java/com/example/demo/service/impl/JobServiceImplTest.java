package com.example.demo.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.demo.constant.JobStatus;
import com.example.demo.constant.MessageCode;
import com.example.demo.dto.request.CreateJobRequestDto;
import com.example.demo.dto.response.JobDetailResponseDto;
import com.example.demo.dto.response.JobDetailPage;
import com.example.demo.dto.response.JobResponseDto;
import com.example.demo.entity.Job;
import com.example.demo.entity.TypeJob;
import com.example.demo.exception.CustomException;
import com.example.demo.repository.JobExecutionLogRepository;
import com.example.demo.repository.JobRepository;
import com.example.demo.repository.TypeJobRepository;
import com.example.demo.service.JobExecutionService;
import com.example.demo.utils.MessageUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

  @Mock
  private JobRepository jobRepository;

  @Mock
  private TypeJobRepository typeJobRepository;

  @Mock
  private MessageUtil messageUtil;

  @Mock
  private JobExecutionService jobExecutionService;

  @Mock
  private TransactionTemplate transactionTemplate;

  @Mock
  private JobExecutionLogRepository jobExecutionLogRepository;

  @InjectMocks
  private JobServiceImpl jobService;

  @BeforeEach
  void setUp() {
    // Mock the TransactionTemplate to just execute the callback immediately
    lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
      TransactionCallback<?> callback = invocation.getArgument(0);
      return callback.doInTransaction(null);
    });
  }

  @Test
  void createJob_Success() {
    CreateJobRequestDto request = new CreateJobRequestDto();
    request.setType("EMAIL");
    request.setPayload(Map.of("to", "test@example.com"));

    TypeJob typeJob = new TypeJob();
    typeJob.setType("EMAIL");

    Job savedJob = new Job();
    savedJob.setId(1);
    savedJob.setStatus(JobStatus.PENDING);

    when(typeJobRepository.findByType("EMAIL")).thenReturn(typeJob);
    when(jobRepository.save(any(Job.class))).thenReturn(savedJob);

    JobResponseDto response = jobService.createJob(request);

    assertNotNull(response);
    assertEquals(1, response.getId());
    assertEquals("PENDING", response.getStatus());
    verify(jobRepository, times(1)).save(any(Job.class));
  }

  @Test
  void getJobById_Success() {
    Job job = new Job();
    job.setId(1);
    TypeJob typeJob = new TypeJob();
    typeJob.setType("EMAIL");
    job.setTypeJob(typeJob);
    job.setStatus(JobStatus.PENDING);

    when(jobRepository.findById(1)).thenReturn(Optional.of(job));
    when(jobExecutionLogRepository.countByJobIdAndStatus(1, JobStatus.FAILED)).thenReturn(2L);

    JobDetailResponseDto response = jobService.getJobById(1);

    assertNotNull(response);
    assertEquals(1, response.getId());
    assertEquals("EMAIL", response.getType());
    assertEquals(2, response.getRetryCount());
  }

  @Test
  void getJobById_NotFound_ThrowsException() {
    when(jobRepository.findById(99)).thenReturn(Optional.empty());
    when(messageUtil.getMessage(MessageCode.MESSAGE_NOT_FOUND)).thenReturn("Not Found");

    CustomException ex = assertThrows(CustomException.class, () -> jobService.getJobById(99));
    assertEquals(MessageCode.MESSAGE_NOT_FOUND.getCode(), ex.getCode());
  }

  @Test
  void getJobs_Success() {
    Job job = new Job();
    job.setId(1);
    TypeJob typeJob = new TypeJob();
    typeJob.setType("EMAIL");
    job.setTypeJob(typeJob);
    job.setStatus(JobStatus.PENDING);

    Page<Job> jobPage = new PageImpl<>(List.of(job), PageRequest.of(0, 20), 1);
    when(jobRepository.findByStatus(eq(JobStatus.PENDING), any())).thenReturn(jobPage);

    JobDetailPage responsePage = jobService.getJobs("PENDING", PageRequest.of(0, 20));

    assertNotNull(responsePage);
    assertEquals(1, responsePage.getTotalRows());
    assertEquals(1, responsePage.getJobDetailResponseDtos().size());
  }

  @Test
  void processPendingJobs_Concurrency_EmptyList() {
    // Simulate another thread locked all jobs, returning empty
    when(jobRepository.findPendingJobsForProcessing(10)).thenReturn(new ArrayList<>());

    List<JobDetailResponseDto> processed = jobService.processPendingJobs();

    assertTrue(processed.isEmpty());
    
    // Verify execution service was NEVER called, preventing duplicate processing
    try {
      verify(jobExecutionService, never()).process(anyInt());
    } catch (Exception e) {
      fail("Should not throw exception during verification");
    }
  }
}
