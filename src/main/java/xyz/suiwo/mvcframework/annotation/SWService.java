package xyz.suiwo.mvcframework.annotation;


import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SWService {
    String value() default "";
}
