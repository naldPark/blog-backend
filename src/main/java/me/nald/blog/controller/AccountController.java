package me.nald.blog.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import me.nald.blog.annotation.PermissionCallable;
import me.nald.blog.annotation.WithoutJwtCallable;
import me.nald.blog.data.dto.AccountRequest;
import me.nald.blog.data.dto.AccountStatusRequestDto;
import me.nald.blog.data.vo.Authority;
import me.nald.blog.response.ResponseObject;
import me.nald.blog.service.AccountService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.Callable;

@AllArgsConstructor
@RestController
@RequestMapping("user")
public class AccountController {

  private final AccountService accountService;

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

  @WithoutJwtCallable
  @Operation(summary = "RSA PUBLIC KEY 제공", description = "RSA PUBLIC KEY 제공")
  @GetMapping("/rsa")
  public Callable<ResponseObject> getRsaPublicKey() {
    ResponseObject responseObject = new ResponseObject();
    String vo = accountService.getRsaData();
    responseObject.putData(vo);
    return () -> responseObject;
  }
  @PutMapping(value = "/editPassword")
  public Callable<ResponseObject> editPassword(@RequestBody AccountRequest accountRequest) {
    return () -> accountService.editPassword(accountRequest);
  }

  @PermissionCallable(authority = Authority.SUPER)
  @PutMapping(value = "/changeStatus")
  public Callable<ResponseObject> changeStatus( @RequestBody AccountStatusRequestDto accountStatusRequest) {
    return () -> accountService.changeStatus(accountStatusRequest);
  }

  @PermissionCallable(authority = Authority.SUPER)
  @PostMapping("/createUser")
  public Callable<ResponseObject> createUser(@Valid @RequestBody AccountRequest accountRequest) {
    return () -> accountService.createUser(accountRequest);
  }

  @PermissionCallable(authority = Authority.SUPER)
  @PutMapping("/editUser")
  public Callable<ResponseObject> editUser(@RequestBody AccountRequest accountRequest) {
    return () -> accountService.editUser(accountRequest);
  }
}
