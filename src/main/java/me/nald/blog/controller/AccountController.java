package me.nald.blog.controller;

import lombok.AllArgsConstructor;
import me.nald.blog.annotation.RequireAuthAll;
import me.nald.blog.annotation.RequireAuthSuper;
import me.nald.blog.annotation.WithoutJwtCallable;
import me.nald.blog.data.dto.AccountDto;
import me.nald.blog.data.model.AccountRequest;
import me.nald.blog.data.model.AccountStatusRequest;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.service.AccountService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static java.util.Arrays.asList;

@AllArgsConstructor
@RestController
@RequestMapping("")
public class AccountController {

    private final AccountService accountService;


    @RequireAuthAll
    @GetMapping("/list")
    public Callable<Object> getUserList(HttpServletRequest request) {
        return () -> accountService.getUserList();
    }

    @WithoutJwtCallable
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Callable<Object> checkUser(HttpServletRequest request, @RequestBody AccountRequest accountRequest) {
        HttpSession httpSession = request.getSession();
        return () -> accountService.getLogin(accountRequest);
    }

    @PutMapping(value = "/editPassword")
    public Callable<Object> editPassword(HttpServletRequest request,  @RequestBody AccountRequest accountRequest) {
        return () -> accountService.editPassword(accountRequest);
    }

    @RequireAuthSuper
    @PutMapping(value = "/changeStatus")
    public Callable<Object> changeStatus(HttpServletRequest request, @RequestBody AccountStatusRequest accountStatusRequest) {
        return () -> accountService.changeStatus(accountStatusRequest);
    }

    @RequireAuthSuper
    @PostMapping("/createUser")
    public Callable<Object> createUser(@Valid  @RequestBody AccountRequest accountRequest) {
        return () -> accountService.createUser(accountRequest);
    }

}
