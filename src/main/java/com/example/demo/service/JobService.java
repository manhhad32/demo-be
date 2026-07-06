package com.example.demo.service;

import com.example.demo.dto.request.CreateJobRequestDto;
import com.example.demo.dto.response.JobDetailPage;
import com.example.demo.dto.response.JobDetailResponseDto;
import com.example.demo.dto.response.JobResponseDto;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface JobService {

  JobResponseDto createJob(CreateJobRequestDto requestDto);

  JobDetailResponseDto getJobById(Integer id);

  JobDetailPage getJobs(String status, Pageable pageable);

  List<JobDetailResponseDto> processPendingJobs();
}
