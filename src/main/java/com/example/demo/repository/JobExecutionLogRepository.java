package com.example.demo.repository;

import com.example.demo.constant.JobStatus;
import com.example.demo.entity.JobExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLog, Integer> {

  @Query("SELECT COUNT(l) FROM JobExecutionLog l WHERE l.job.id = :jobId AND l.status = :status")
  long countByJobIdAndStatus(@Param("jobId") Integer jobId, @Param("status") JobStatus status);
}
