package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.CurrentDateTimeProvider;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "dateAuditing")
public class AuditInfoConfig {
  @Bean
  public DateTimeProvider dateAuditing() {
    return CurrentDateTimeProvider.INSTANCE;
  }
}
