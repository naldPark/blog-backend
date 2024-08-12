package me.nald.blog.annotation;


import me.nald.blog.data.vo.Authority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PermissionCallable {
  Authority authority();

  String key() default "";
  Class<?> value() default Void.class;
}
