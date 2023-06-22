package me.nald.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableAsync
public class BlogApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlogApplication.class, args);
	}

//	@Scheduled(cron = "0 0 1 * * *") // 01:00시마다 token 체크
//	public void checkToken() {
//		authService.tokenExpiredCheck();
//	}

}
