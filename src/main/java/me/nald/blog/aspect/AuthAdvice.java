package me.nald.blog.aspect;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.annotation.WithoutJwtCallable;
import me.nald.blog.data.persistence.entity.Account;
import me.nald.blog.data.vo.YN;
import me.nald.blog.exception.ErrorSpec;
import me.nald.blog.exception.Errors;
import me.nald.blog.service.AccountService;
import me.nald.blog.util.Util;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Objects;

import static me.nald.blog.util.Constants.*;

@Component
@Aspect
@Order
@Slf4j
@AllArgsConstructor
public class AuthAdvice {

    private final AccountService accountService;


    @Before("Pointcuts.allController()")
    public void checkBeforeController(JoinPoint jp) {

        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        System.out.println("== AuthAdvice ==");

        MethodSignature signature = (MethodSignature) jp.getSignature();
        Method method = signature.getMethod();
        WithoutJwtCallable withoutJwtCallable = method.getDeclaredAnnotation(WithoutJwtCallable.class);


        if (Objects.nonNull(withoutJwtCallable)) {
            request.setAttribute(ANONYMOUS_YN, YN.Y.name());
            return;
        }

        String userId = Util.extractUserIdFromJwt(request);
        System.out.println("유저아이디"+ userId);
        Account user = accountService.findMemberByAccountId(userId);
        if (user != null) {
            request.setAttribute(USER_ID, user.getAccountId());
            request.setAttribute(AUTHORITIES, user.getAuthority());
        }else{
           throw Errors.of(ErrorSpec.AccessDeniedException);
        }


    }

}
