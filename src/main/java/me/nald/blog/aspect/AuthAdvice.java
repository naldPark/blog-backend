package me.nald.blog.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Order
@Slf4j
public class AuthAdvice {


    @Before("Pointcuts.allController()")
    public void checkBeforeController(JoinPoint jp) {

        System.out.println("== AuthAdvice ==");
    }

}
