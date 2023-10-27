package me.nald.blog;

import me.nald.blog.service.CommonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableAsync
public class BlogApplication {

	@Autowired
	CommonService commonService;

	public static void main(String[] args) {
		SpringApplication.run(BlogApplication.class, args);
	}

	@Scheduled(cron = "0 0 1 * * *") // 01:00시마다 메일보낸횟수 초기화
	public void checkToken() {
		commonService.mailCountReset();
	}

}
