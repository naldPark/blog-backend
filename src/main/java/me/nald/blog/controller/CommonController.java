package me.nald.blog.controller;

import lombok.AllArgsConstructor;
import me.nald.blog.annotation.WithoutJwtCallable;
import me.nald.blog.data.dto.ContactRequestDto;
import me.nald.blog.response.ResponseObject;
import me.nald.blog.service.CommonService;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.Callable;

@AllArgsConstructor
@RestController
@RequestMapping("/common")
public class CommonController {

    private final CommonService commonService;


    @WithoutJwtCallable
    @PostMapping("/mail")
    public Callable<ResponseObject> sendMail(@RequestBody ContactRequestDto contactRequest) {
        return () -> commonService.sendMail(contactRequest);
    }
    // 이거는 반복 스팸을 방지하기 위해 힙 메모리에 아이피를 일시적으로 저장하고 5회 이상 반복될 시 차단

    @GetMapping("/blog/list")
    public Callable<ResponseObject> getBlogList() {
        return () -> commonService.getBlogList();
    }

    @WithoutJwtCallable
    @GetMapping("/badge/list")
    public Callable<ResponseObject> getBadgeList() {
        return () -> commonService.getBadgeList();
    }

}
