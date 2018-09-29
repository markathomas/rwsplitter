package com.elihullc.rwsplitter.jpa.aop;

import com.elihullc.rwsplitter.jpa.hibernate.CurrentDatabaseRole;
import com.elihullc.rwsplitter.jpa.hibernate.DatabaseRole;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aspect that intercepts all public {@link Transactional} methods and set the {@link DatabaseRole} on the current thread based on
 * the value of {@link Transactional#readOnly()}.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseRoleInterceptor implements PriorityOrdered {

    private int order = Ordered.HIGHEST_PRECEDENCE;

    @Pointcut(value = "execution(public * *(..))")
    public void anyPublicMethod() { }

    @Around("@annotation(txAnnotation))")
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
    @Value("20")
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
