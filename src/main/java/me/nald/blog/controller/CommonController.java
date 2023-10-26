package me.nald.blog.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.annotation.WithoutJwtCallable;
import me.nald.blog.data.model.ContactRequest;
import me.nald.blog.service.CommonService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.Callable;

@AllArgsConstructor
@RestController
@RequestMapping("/common")
@Slf4j
public class CommonController {

    private final CommonService commonService;


    @WithoutJwtCallable
    @PostMapping("/sendMail")
    public Callable<Object> sendMail(@RequestBody ContactRequest contactRequest) {
        return () -> commonService.sendMail(contactRequest);
    }

    @GetMapping("/blogList")
    public Callable<Object> getBlogList() {
        return () -> commonService.getBlogList();
    }

    @WithoutJwtCallable
    @GetMapping("/badgeList")
    public Callable<Object> getBadgeList() {
        return () -> commonService.getBadgeList();
    }

}
