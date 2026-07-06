package com.example.demo.repository;

import com.example.demo.constant.JobStatus;
import com.example.demo.entity.Job;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Integer> {

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    @Query(value = "SELECT * FROM job WHERE status = 'PENDING' AND (next_run_at IS NULL OR next_run_at <= NOW()) FOR UPDATE SKIP LOCKED LIMIT :limit", nativeQuery = true)
    List<Job> findPendingJobsForProcessing(@Param("limit") int limit);
}
