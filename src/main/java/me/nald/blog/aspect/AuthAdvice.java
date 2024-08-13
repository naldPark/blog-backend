package me.nald.blog.aspect;

import lombok.extern.slf4j.Slf4j;
import me.nald.blog.annotation.*;
import me.nald.blog.data.entity.Account;
import me.nald.blog.data.vo.AccountVo;
import me.nald.blog.data.vo.YN;
import me.nald.blog.exception.AuthException;
import me.nald.blog.response.ResponseCode;
import me.nald.blog.service.AccountService;
import me.nald.blog.util.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

import static me.nald.blog.util.Constants.*;

@Component
@Aspect
@Order
@Slf4j
public class AuthAdvice {
  private static final Logger logger = LogManager.getLogger(AuthAdvice.class);


  @Autowired
  private AccountService accountService;

  @Autowired
  public void setAuthService(AccountService accountService) {
    this.accountService = accountService;
  }


  @Before("Pointcuts.allController()")
  public void checkBeforeController(JoinPoint jp) throws NoSuchAlgorithmException, InvalidKeySpecException {

    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

    MethodSignature signature = (MethodSignature) jp.getSignature();
    Method method = signature.getMethod();
    WithoutJwtCallable withoutJwtCallable = method.getDeclaredAnnotation(WithoutJwtCallable.class);

    if (Objects.nonNull(withoutJwtCallable)) {
      request.setAttribute(ANONYMOUS_YN, YN.Y.name());
    } else {
      AccountVo jwt = CommonUtils.extractUserIdFromJwt(request);
      Account user = accountService.findMemberByAccountId(jwt.getAccountId());

/**
 * getDeclaredAnnotation은 메서드에 직접적으로 존재하는 어노테이션만을 검색
 * getMethodAnnotation은 상속된 어노테이션을 포함하여 지정된 어노테이션 타입을 검색
 * **/
      PermissionCallable permissionCallable = method.getDeclaredAnnotation(PermissionCallable.class);

      if (Objects.nonNull(permissionCallable)) {
        int authNumber = permissionCallable.authority().getNum();
        if (authNumber < jwt.getAuthority()) {
          new AuthException(logger, ResponseCode.ACCESS_DENIED);
        }
      }
      if (user != null) {
        request.setAttribute(USER_ID, user.getAccountId());
        request.setAttribute(AUTHORITY, user.getAuthority());
      } else {
        new AuthException(logger, ResponseCode.ACCESS_DENIED);
      }
    }
  }

}
