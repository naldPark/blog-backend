package me.nald.blog.annotation;

import me.nald.blog.data.persistence.entity.Account;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequireAuthBiz {

    Account.Authority value() default Account.Authority.BIZ;
}
