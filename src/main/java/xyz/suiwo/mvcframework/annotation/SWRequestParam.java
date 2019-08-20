package xyz.suiwo.mvcframework.annotation;


import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SWRequestParam {
    String value() default "";
    boolean required() default true;
}
