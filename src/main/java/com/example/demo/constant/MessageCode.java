package com.example.demo.constant;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageCode {

  MESSAGE_NOT_FOUND("NOT_FOUND"),
  MESSAGE_ERROR_SYSTEM_ERROR("SYSTEM_ERROR"),
  MESSAGE_ERROR_INPUT_ERROR("INPUT_ERROR"),
  MESSAGE_TYPE_JOB_NOT_FOUND("JOB_404"),
  MESSAGE_JOB_PROCESSING_FAIL("JOB_PROCESSING_FAIL");


  private final String code;

  @Nullable
  public static MessageCode fromCode(String code) {
    for (MessageCode messageCode : MessageCode.values()) {
      if (messageCode.getCode().equals(code)) {
        return messageCode;
      }
    }
    return null;
  }
}
