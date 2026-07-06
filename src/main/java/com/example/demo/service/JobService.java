package com.example.demo.service;

import com.example.demo.dto.request.CreateJobRequestDto;
import com.example.demo.dto.response.JobResponseDto;

public interface JobService {

  JobResponseDto createJob(CreateJobRequestDto requestDto);

}
