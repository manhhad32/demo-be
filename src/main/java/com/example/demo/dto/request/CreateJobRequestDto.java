package com.example.demo.dto.request;

import java.util.Map;
import lombok.Data;

@Data
public class CreateJobRequestDto {

  private String type;
  private Map<String, Object> payload;


}
