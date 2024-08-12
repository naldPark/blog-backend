package me.nald.blog.aspect;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.nald.blog.config.BlogProperties;
import me.nald.blog.util.Constants;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

@Order
@Slf4j
@AllArgsConstructor
@Component
@Aspect
public class LogAdvice {


    @Before("Pointcuts.allController()")
    public void writeBeforeController(JoinPoint jp) {
        writeBeforeLog(jp, "Controller");
    }

    @Before("Pointcuts.allService()")
    public void writeBeforeService(JoinPoint jp) {
        writeBeforeLog(jp, "Service");
    }

    private void writeBeforeLog(JoinPoint jp, String identifier) {
        log.info("@{}(ver:{}): {}.{}()",
                identifier,
                Optional.ofNullable(jp)
                        .map(JoinPoint::getTarget)
                        .map(Object::getClass)
                        .map(Class::getSimpleName)
                        .orElse("????????"),
                jp.getSignature().getName()
        );
        log.info(" Args : {}", getArgsAsString(jp.getArgs()));
    }

    private Object writeAroundLog(ProceedingJoinPoint joinPoint, String identifier) throws Throwable {
        long start = System.currentTimeMillis();
        log.info("@{}(ver:{}): {}.{} / startedAt: {}",
                identifier,
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                Constants.DEFAULT_DATETIME_FORMAT.format(new Date(start))
        );
        log.info(" Args: {}", getArgsAsString(joinPoint.getArgs()));
        Object returnValue = joinPoint.proceed();
        long end = System.currentTimeMillis();
        log.info("@{}(ver:{}): {}.{} / endAt: {}, executionTime: {} ms",
                identifier,
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                Constants.DEFAULT_DATETIME_FORMAT.format(new Date(end)),
                (end - start)
        );
        log.info(" Return Value: {}", Optional.ofNullable(returnValue).map(Object::toString).orElse(""));
        log.info(" ### executionTime: {} ms", (end - start));
        return returnValue;
    }

    private String getArgsAsString(Object[] objects) {
        return Arrays.stream(objects)
                .map(obj -> Optional.ofNullable(obj).map(o -> o.getClass().getSimpleName() + " >> " + o.toString()).orElse("? >> null"))
                .collect(Collectors.joining(", "));
    }
}
