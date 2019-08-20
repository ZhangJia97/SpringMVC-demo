package xyz.suiwo.mvcframework.annotation;


import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SWRequestMapping {
    String value() default "";
}
