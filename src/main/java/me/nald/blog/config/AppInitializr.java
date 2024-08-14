package me.nald.blog.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.data.entity.Account;
import me.nald.blog.data.entity.Password;
import me.nald.blog.repository.AccountRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static me.nald.blog.util.SecurityUtils.encryptSHA256;

@Slf4j
@RequiredArgsConstructor
@Component
public class AppInitializr {
    private final AccountRepository accountRepository;
    private final BlogProperties blogProperties;

    @Bean
    public ApplicationRunner initAccount() {
        return args -> {
            if (accountRepository.findAll().isEmpty()) {
                /** TODO : privateKey 암호화 **/
                String initPw = encryptSHA256(blogProperties.getDefaultAccountPassword());
                Password password = Password.builder()
                        .password(initPw)
                        .build();

                Account account = Account.createAccount(
                        blogProperties.getDefaultAccountId(),
                        blogProperties.getDefaultAccountId(),
                        blogProperties.getContactEmail(),
                        0,
                        password,
                        0
                );
                accountRepository.save(account);
            }
        };
    }

    @Bean
    public ThreadPoolTaskExecutor executeServiceThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(20);
        taskExecutor.setMaxPoolSize(150);
        taskExecutor.setThreadNamePrefix("K8s-Executor-");
        taskExecutor.setQueueCapacity(150);
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Bean
    public RestTemplate restTemplateForStorageService(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofMinutes(30))
                .setReadTimeout(Duration.ofMinutes(30))
                .build();
    }


}
