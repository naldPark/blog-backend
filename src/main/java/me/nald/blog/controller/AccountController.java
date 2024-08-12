package me.nald.blog.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import me.nald.blog.annotation.RequireAuthAll;
import me.nald.blog.annotation.RequireAuthSuper;
import me.nald.blog.annotation.WithoutJwtCallable;
import me.nald.blog.data.dto.AccountRequest;
import me.nald.blog.data.dto.AccountStatusRequestDto;
import me.nald.blog.response.ResponseObject;
import me.nald.blog.service.AccountService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.concurrent.Callable;

@AllArgsConstructor
@RestController
@RequestMapping("")
public class AccountController {

  private final AccountService accountService;

  @RequireAuthAll
  @GetMapping("/list")
  public Callable<ResponseObject> getUserList() {
    ResponseObject responseObject = new ResponseObject();
    responseObject.putData(accountService.getUserList());
    return () -> responseObject;
  }

  @WithoutJwtCallable
  @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Callable<ResponseObject> checkUser( @RequestBody AccountRequest accountRequest) {
    return () -> accountService.getLogin(accountRequest);
  }

  @PutMapping(value = "/editPassword")
  public Callable<ResponseObject> editPassword(@RequestBody AccountRequest accountRequest) {
    return () -> accountService.editPassword(accountRequest);
  }

  @RequireAuthSuper
  @PutMapping(value = "/changeStatus")
  public Callable<ResponseObject> changeStatus( @RequestBody AccountStatusRequestDto accountStatusRequest) {
    return () -> accountService.changeStatus(accountStatusRequest);
  }

  @RequireAuthSuper
  @PostMapping("/createUser")
  public Callable<ResponseObject> createUser(@Valid @RequestBody AccountRequest accountRequest) {
    return () -> accountService.createUser(accountRequest);
  }

  @RequireAuthSuper
  @PutMapping("/editUser")
  public Callable<ResponseObject> editUser(@RequestBody AccountRequest accountRequest) {
    return () -> accountService.editUser(accountRequest);
  }
}
