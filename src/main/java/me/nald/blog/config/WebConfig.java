package me.nald.blog.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class WebConfig {
    private final BlogProperties blogProperties;

    @Bean
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setDefaultEncoding("utf-8");
        resolver.setMaxUploadSize(10737418240L); // 10GB
        resolver.setMaxInMemorySize(1048576); // 1MB
        try {
            resolver.setUploadTempDir(new FileSystemResource(blogProperties.getCommonPath() +"/tomcat/temp"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resolver;
    }
}
