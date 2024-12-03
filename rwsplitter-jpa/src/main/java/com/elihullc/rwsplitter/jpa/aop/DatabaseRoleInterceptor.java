package com.elihullc.rwsplitter.jpa.aop;

import com.elihullc.rwsplitter.jpa.CurrentDatabaseRole;
import com.elihullc.rwsplitter.jpa.DatabaseRole;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.PriorityOrdered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aspect that intercepts all public {@link Transactional} methods and sets the {@link DatabaseRole} on the current thread based on
 * the value of {@link Transactional#readOnly()}.
 */
@Aspect
@Component
public class DatabaseRoleInterceptor implements PriorityOrdered {

    private int order = 20;

    @Pointcut(value = "execution(public * *(..))")
    public void anyPublicMethod() { }

    @Around(value = "anyPublicMethod() && @annotation(txAnnotation)", argNames = "txAnnotation")
    public Object around(final ProceedingJoinPoint joinPoint, final Transactional txAnnotation) throws Throwable {
        CurrentDatabaseRole.setCurrentRole(txAnnotation.readOnly() ? DatabaseRole.READER : DatabaseRole.WRITER);
        try {
            return joinPoint.proceed();
        } finally {
            CurrentDatabaseRole.resetCurrentRole();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOrder() {
        return this.order;
    }
}
