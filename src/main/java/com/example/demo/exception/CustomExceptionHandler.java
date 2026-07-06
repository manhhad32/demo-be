package com.example.demo.exception;


import com.example.demo.constant.MessageCode;
import com.example.demo.dto.response.ErrorMessageResponseDTO;
import com.example.demo.utils.MessageUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;


@ControllerAdvice()
@Slf4j
public class CustomExceptionHandler {

  private final MessageUtil messageUtil;

  @Autowired
  public CustomExceptionHandler(MessageUtil messageUtil) {
    this.messageUtil = messageUtil;
  }

  HttpStatus errorCodeToHttpStatus(MessageCode errorCode) {
    return switch (errorCode) {
      case MESSAGE_NOT_FOUND -> HttpStatus.NOT_FOUND;

      case MESSAGE_TYPE_JOB_NOT_FOUND,
           MESSAGE_ERROR_INPUT_ERROR -> HttpStatus.BAD_REQUEST;

      case null, default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }

  @ExceptionHandler({CustomException.class})
  public ResponseEntity<ErrorMessageResponseDTO> customExceptionHandle(CustomException ex) {
    if (!ObjectUtils.isEmpty(ex.getCode())) {
      HttpStatus httpStatus = errorCodeToHttpStatus(MessageCode.fromCode(ex.getCode()));
      String errorMessage = ex.getMessage() == null ? httpStatus.toString() : ex.getMessage();
      ErrorMessageResponseDTO messageError =
          new ErrorMessageResponseDTO(ex.getCode(), errorMessage, ex.getRequestId());
      return new ResponseEntity<>(messageError, httpStatus);
    } else {
      return new ResponseEntity<>(
          new ErrorMessageResponseDTO(
              MessageCode.MESSAGE_ERROR_SYSTEM_ERROR.getCode(),
              messageUtil.getMessage(MessageCode.MESSAGE_ERROR_SYSTEM_ERROR),
              ex.getRequestId()),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ResponseEntity<ErrorMessageResponseDTO> handleNotFoundException(NoHandlerFoundException ex,
      HttpServletRequest request) {
    ErrorMessageResponseDTO messageError = new ErrorMessageResponseDTO(
        MessageCode.MESSAGE_NOT_FOUND.getCode(),
        messageUtil.getMessage(MessageCode.MESSAGE_NOT_FOUND));
    return new ResponseEntity<>(messageError, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ResponseEntity<ErrorMessageResponseDTO> handleAllUncaughtException(
      Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception occurred: ", ex);
    ErrorMessageResponseDTO messageError = new ErrorMessageResponseDTO(
        MessageCode.MESSAGE_ERROR_SYSTEM_ERROR.getCode(),
        messageUtil.getMessage(MessageCode.MESSAGE_ERROR_SYSTEM_ERROR));
    return new ResponseEntity<>(messageError, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
