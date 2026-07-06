package com.example.demo.utils;


import com.example.demo.constant.MessageCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class MessageUtil {

  private final MessageSource messageSource;

  @Autowired
  MessageUtil(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public String getMessage(MessageCode key, Object... params) {
    return messageSource.getMessage(key.getCode(), params, LocaleContextHolder.getLocale());
  }
}
