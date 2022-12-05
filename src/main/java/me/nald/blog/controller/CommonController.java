package me.nald.blog.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.annotation.WithoutJwtCallable;
import me.nald.blog.data.model.ContactRequest;
import me.nald.blog.service.CommonService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        System.out.println("메일보내기");
        return () -> commonService.sendMail(contactRequest);
    }

}
