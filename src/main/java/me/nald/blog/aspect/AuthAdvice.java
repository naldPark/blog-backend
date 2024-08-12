package me.nald.blog.aspect;

import lombok.extern.slf4j.Slf4j;
import me.nald.blog.annotation.*;
import me.nald.blog.data.entity.Account;
import me.nald.blog.data.vo.AccountVO;
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

    System.out.println("== AuthAdvice ==");

    MethodSignature signature = (MethodSignature) jp.getSignature();
    Method method = signature.getMethod();
    WithoutJwtCallable withoutJwtCallable = method.getDeclaredAnnotation(WithoutJwtCallable.class);

    if (Objects.nonNull(withoutJwtCallable)) {
      request.setAttribute(ANONYMOUS_YN, YN.Y.name());
    } else {
      AccountVO jwt = CommonUtils.extractUserIdFromJwt(request);
      Account user = accountService.findMemberByAccountId(jwt.getAccountId());

//      RequireAuthAll requireAuthAll = method.getDeclaredAnnotation(RequireAuthAll.class);
//      RequireAuthBuddy requireAuthBuddy = method.getDeclaredAnnotation(RequireAuthBuddy.class);
//      RequireAuthBiz requireAuthBiz = method.getDeclaredAnnotation(RequireAuthBiz.class);
//      RequireAuthSuper requireAuthSuper = method.getDeclaredAnnotation(RequireAuthSuper.class);
//      if (Objects.nonNull(requireAuthSuper)) {
//        if (requireAuthSuper.value().ordinal() < jwt.getAuthority()) {
//          new AuthException(logger, ResponseCode.ACCESS_DENIED);
//        }
//      }
//      if (Objects.nonNull(requireAuthAll)) {
//        if (requireAuthAll.value().ordinal() < jwt.getAuthority()) {
//          new AuthException(logger, ResponseCode.ACCESS_DENIED);
//        }
//      }
//      if (Objects.nonNull(requireAuthBuddy)) {
//        if (requireAuthBuddy.value().ordinal() < jwt.getAuthority()) {
//          new AuthException(logger, ResponseCode.ACCESS_DENIED);
//        }
//      }
//      if (Objects.nonNull(requireAuthBiz)) {
//        if (jwt.getAuthority() != 2
//                && requireAuthBiz.value().ordinal() < jwt.getAuthority()) {
//          new AuthException(logger, ResponseCode.ACCESS_DENIED);
//        }
//      }

      if (user != null) {
        request.setAttribute(USER_ID, user.getAccountId());
        request.setAttribute(AUTHORITY, user.getAuthority());
      } else {
        new AuthException(logger, ResponseCode.ACCESS_DENIED);
      }
    }
  }

}
