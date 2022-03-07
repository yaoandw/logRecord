package cn.monitor4all.logRecord.annotation;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLogReactive {
    String bizId();

    String bizType();

    String msg() default "";

    String tag() default "";
}
