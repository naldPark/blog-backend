package me.nald.blog.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Aspect
@Order
public class Pointcuts {

    @Pointcut("execution(* me.nald.blog.controller..*.*(..))")
    public void allController() {
    }

    @Pointcut("execution(* me.nald.blog.service..*.*(..))")
    public void allService() {
    }

}
