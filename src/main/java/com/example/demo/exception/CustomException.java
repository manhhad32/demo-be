package com.example.demo.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

  private final String code;
  private final String message;
  private final String requestId;

  public CustomException(String code, String message) {
    super(message);
    this.code = code;
    this.message = message;
    this.requestId = null;
  }

  public CustomException(String code, String message, String requestId) {
    super(message);
    this.code = code;
    this.message = message;
    this.requestId = requestId;
  }

  public CustomException() {
    this.code = "";
    this.message = "";
    this.requestId = "";
  }
}
