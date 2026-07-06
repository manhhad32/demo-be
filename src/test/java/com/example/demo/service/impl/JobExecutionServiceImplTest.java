package com.example.demo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.demo.constant.JobStatus;
import com.example.demo.entity.Job;
import com.example.demo.entity.JobExecutionLog;
import com.example.demo.entity.TypeJob;
import com.example.demo.repository.JobExecutionLogRepository;
import com.example.demo.repository.JobRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobExecutionServiceImplTest {

  @Mock
  private JobRepository jobRepository;

  @Mock
  private JobExecutionLogRepository logRepository;

  @InjectMocks
  private JobExecutionServiceImpl jobExecutionService;

  @Test
  void process_Success() {
    Job job = new Job();
    job.setId(1);
    TypeJob typeJob = new TypeJob();
    typeJob.setType("EMAIL");
    job.setTypeJob(typeJob);

    when(jobRepository.findById(1)).thenReturn(Optional.of(job));

    jobExecutionService.process(1);

    assertEquals(JobStatus.COMPLETED, job.getStatus());
    verify(jobRepository, times(1)).save(job);
    verify(logRepository, times(1)).save(any(JobExecutionLog.class));
  }

  @Test
  void process_SimulatedFailure_ThrowsException() {
    Job job = new Job();
    job.setId(1);
    job.setPayload(Map.of("fail", true)); // Trigger simulated failure

    when(jobRepository.findById(1)).thenReturn(Optional.of(job));

    assertThrows(RuntimeException.class, () -> jobExecutionService.process(1));

    // Shouldn't save anything on process failure (since it's meant to be retried/recovered)
    verify(jobRepository, never()).save(any(Job.class));
    verify(logRepository, never()).save(any(JobExecutionLog.class));
  }

  @Test
  void recover_RetryCountLessThan3_SetsToPendingAndUpdatesNextRunAt() {
    Job job = new Job();
    job.setId(1);

    when(jobRepository.findById(1)).thenReturn(Optional.of(job));
    // Simulate 1 previous failure
    when(logRepository.countByJobIdAndStatus(1, JobStatus.FAILED)).thenReturn(1L);

    RuntimeException ex = new RuntimeException("Test Exception");
    jobExecutionService.recover(ex, 1);

    assertEquals(JobStatus.PENDING, job.getStatus());
    assertNotNull(job.getNextRunAt());

    verify(jobRepository, times(1)).save(job);
    verify(logRepository, times(1)).save(argThat(log -> log.getStatus() == JobStatus.FAILED));
  }

  @Test
  void recover_MaxRetryReached_SetsToFailed() {
    Job job = new Job();
    job.setId(1);

    when(jobRepository.findById(1)).thenReturn(Optional.of(job));
    // Simulate 2 previous failures + this current one = 3
    when(logRepository.countByJobIdAndStatus(1, JobStatus.FAILED)).thenReturn(2L);

    RuntimeException ex = new RuntimeException("Test Exception");
    jobExecutionService.recover(ex, 1);

    assertEquals(JobStatus.FAILED, job.getStatus());
    assertNull(job.getNextRunAt());

    verify(jobRepository, times(1)).save(job);
    verify(logRepository, times(1)).save(argThat(log -> log.getStatus() == JobStatus.FAILED));
  }
}
