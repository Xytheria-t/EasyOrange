package com.cartethyia.easyorange.user.event.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublishEvent {
    
    String type();
    
    String extractor() default "";
    
    boolean afterTransaction() default true;
}
