package com.example.demo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class ErrorMessageResponseDTO {

  private String errorCode;
  private String errorMessage;
  private String requestId;

  public ErrorMessageResponseDTO(String errorCode, String errorMessage, String requestId) {
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
    this.requestId = requestId;
  }

  public ErrorMessageResponseDTO(String errorCode, String errorMessage) {
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }
}
