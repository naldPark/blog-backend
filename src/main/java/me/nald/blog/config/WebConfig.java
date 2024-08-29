package me.nald.blog.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class WebConfig {
    private final BlogProperties blogProperties;

    // multipart 관련 config 설정
//    @Bean
//    public CommonsMultipartResolver multipartResolver() {
//        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
//        resolver.setDefaultEncoding("utf-8");
//        resolver.setMaxUploadSize(-1); // 10GB
//        resolver.setMaxInMemorySize(-1); // 1MB
//        try {
//            // tomcat에서 임시 파일 저장소 지정
//            resolver.setUploadTempDir(new FileSystemResource(blogProperties.getCommonPath() +blogProperties.getTomcatTempFilePath()));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return resolver;
//    }
}
