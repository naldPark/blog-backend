package me.nald.blog.aspect;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
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

    private String getArgsAsString(Object[] objects) {
        return Arrays.stream(objects)
                .map(obj -> Optional.ofNullable(obj).map(o -> o.getClass().getSimpleName() + " >> " + o.toString()).orElse("? >> null"))
                .collect(Collectors.joining(", "));
    }
}
