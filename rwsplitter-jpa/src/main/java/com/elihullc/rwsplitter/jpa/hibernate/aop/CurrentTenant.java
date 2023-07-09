package com.elihullc.rwsplitter.jpa.hibernate.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to specify the current tenant for a class or method.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentTenant {

    /**
     * Returns the current tenant identifier. Defaults to "master".
     * @return The current tenant identifier
     */
    String value() default "master";
}
