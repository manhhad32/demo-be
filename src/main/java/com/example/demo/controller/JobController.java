package com.example.demo.controller;

import com.example.demo.dto.request.CreateJobRequestDto;
import com.example.demo.dto.response.JobResponseDto;
import com.example.demo.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
