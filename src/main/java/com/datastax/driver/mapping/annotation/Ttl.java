package com.datastax.driver.mapping.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Time to leave.
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Ttl {
	
    /**
     * (Optional) Time to leave in seconds.
     * <p> Defaults to 0 which means never expire.
     */
    int value() default 0;	

}
