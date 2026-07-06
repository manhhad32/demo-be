package com.example.demo.controller;

import com.example.demo.dto.request.CreateJobRequestDto;
import com.example.demo.dto.response.JobDetailPage;
import com.example.demo.dto.response.JobDetailResponseDto;
import com.example.demo.dto.response.JobResponseDto;
import com.example.demo.service.JobService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class JobController {

  private final JobService jobService;

  @Autowired
  public JobController(JobService jobService) {
    this.jobService = jobService;
  }

  @PostMapping("/jobs")
  public ResponseEntity<JobResponseDto> createJob(@RequestBody CreateJobRequestDto requestDto) {
    return new ResponseEntity<>(jobService.createJob(requestDto), HttpStatus.OK);
  }

  @GetMapping("/jobs/{id}")
  public ResponseEntity<JobDetailResponseDto> getJobById(@PathVariable Integer id) {
    return new ResponseEntity<>(jobService.getJobById(id), HttpStatus.OK);
  }

  @GetMapping("/jobs")
  public ResponseEntity<JobDetailPage> getJobs(
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(page, size);
    return new ResponseEntity<>(jobService.getJobs(status, pageable), HttpStatus.OK);
  }

  @PostMapping("/jobs/process")
  public ResponseEntity<List<JobDetailResponseDto>> processJobs() {
    List<JobDetailResponseDto> processedJobs = jobService.processPendingJobs();
    return new ResponseEntity<>(processedJobs, HttpStatus.OK);
  }
}
