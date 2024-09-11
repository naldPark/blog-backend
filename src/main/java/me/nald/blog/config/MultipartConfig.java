package me.nald.blog.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import  jakarta.servlet.MultipartConfigElement;

@Configuration
@RequiredArgsConstructor
public class MultipartConfig {
  private final BlogProperties blogProperties;
  @Bean
  public MultipartConfigElement multipartConfigElement() {
    MultipartConfigFactory factory = new MultipartConfigFactory();

    /* 임시 파일 저장소 경로 설정 */
    factory.setLocation(blogProperties.getCommonPath() + blogProperties.getTomcatTempFilePath());

    /* 멀티파일 최대 크기 설정 */
    factory.setMaxFileSize(DataSize.parse("10GB")); // 최대 파일 크기 설정
    factory.setMaxRequestSize(DataSize.parse("10GB")); // 최대 요청 크기 설정

    return factory.createMultipartConfig();
  }
}
