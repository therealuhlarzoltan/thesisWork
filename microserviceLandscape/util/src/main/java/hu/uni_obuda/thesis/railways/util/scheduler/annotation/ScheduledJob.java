package hu.uni_obuda.thesis.railways.util.scheduler.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ScheduledJob {
    String value();
}
