package me.nald.blog.controller;

import lombok.AllArgsConstructor;
import me.nald.blog.annotation.WithoutJwtCallable;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.service.AccountService;
import me.nald.blog.service.CommonService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.Callable;

import static java.util.Arrays.asList;

@AllArgsConstructor
@RestController
@RequestMapping("common")
public class CommonController {

    private final CommonService commonService;


    @WithoutJwtCallable
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Callable<Object> checkUser(HttpServletRequest request) {
        HttpSession httpSession = request.getSession();
        return () -> commonService.sendMail();
    }

}
