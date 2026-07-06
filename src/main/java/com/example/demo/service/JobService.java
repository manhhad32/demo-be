package com.example.demo.service;

import com.example.demo.dto.request.CreateJobRequestDto;
import com.example.demo.dto.response.JobDetailResponseDto;
import com.example.demo.dto.response.JobDetailPage;
import com.example.demo.dto.response.JobResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface JobService {

  JobResponseDto createJob(CreateJobRequestDto requestDto);

  JobDetailResponseDto getJobById(Integer id);

  JobDetailPage getJobs(String status, Pageable pageable);

  List<JobDetailResponseDto> processPendingJobs();
}
