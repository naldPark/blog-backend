package me.nald.blog.aspect;

import lombok.extern.slf4j.Slf4j;
import me.nald.blog.annotation.*;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.data.vo.AccountVO;
import me.nald.blog.data.vo.YN;
import me.nald.blog.exception.Errors;
import me.nald.blog.service.AccountService;
import me.nald.blog.util.Util;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Objects;

import static me.nald.blog.exception.ErrorSpec.AccessDeniedException;
import static me.nald.blog.util.Constants.*;

@Component
@Aspect
@Order
@Slf4j
public class AuthAdvice {

    @Autowired
    private AccountService accountService;

    @Autowired
    public void setAuthService(AccountService accountService) {
        this.accountService = accountService;
    }


    @Before("Pointcuts.allController()")
    public void checkBeforeController(JoinPoint jp) {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        System.out.println("== AuthAdvice ==");

        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        WithoutJwtCallable withoutJwtCallable = method.getDeclaredAnnotation(WithoutJwtCallable.class);

        if (Objects.nonNull(withoutJwtCallable)) {
            request.setAttribute(ANONYMOUS_YN, YN.Y.name());
        } else {
            AccountVO jwt = Util.extractUserIdFromJwt(request);
            Account user = accountService.findMemberByAccountId(jwt.getAccountId());

            RequireAuthAll requireAuthAll = method.getDeclaredAnnotation(RequireAuthAll.class);
            RequireAuthBuddy requireAuthBuddy = method.getDeclaredAnnotation(RequireAuthBuddy.class);
            RequireAuthBiz requireAuthBiz = method.getDeclaredAnnotation(RequireAuthBiz.class);
            RequireAuthSuper requireAuthSuper = method.getDeclaredAnnotation(RequireAuthSuper.class);
            if (Objects.nonNull(requireAuthSuper)) {
                if (requireAuthSuper.value().ordinal() < jwt.getAuthority()) {
                    throw Errors.of(AccessDeniedException);
                }
            }
            if (Objects.nonNull(requireAuthAll)) {
                if (requireAuthAll.value().ordinal() < jwt.getAuthority()) {
                    throw Errors.of(AccessDeniedException);
                }
            }
            if (Objects.nonNull(requireAuthBuddy)) {
                    if(requireAuthBuddy.value().ordinal() < jwt.getAuthority()) {
                    throw Errors.of(AccessDeniedException);
                }
            }
            if (Objects.nonNull(requireAuthBiz)) {
                if (jwt.getAuthority() != 2
                && requireAuthBiz.value().ordinal() < jwt.getAuthority()) {
                    throw Errors.of(AccessDeniedException);
                }
            }

            if (user != null) {
                request.setAttribute(USER_ID, user.getAccountId());
                request.setAttribute(AUTHORITY, user.getAuthority());
            } else {
                throw Errors.of(AccessDeniedException);
            }
        }
    }

}
