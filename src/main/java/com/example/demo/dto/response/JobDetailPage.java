package com.example.demo.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class JobDetailPage extends PaginatedDTO {

  List<JobDetailResponseDto> jobDetailResponseDtos;
}
