package com.elihullc.rwsplitter.jpa.hibernate.aop;

import com.elihullc.rwsplitter.jpa.hibernate.SpringTenantIdentifierResolver;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Aspect that intercepts all public methods annotated with {@link CurrentTenant} and sets the tenant on the current thread based on
 * the value of {@link CurrentTenant#value()}.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantSettingInterceptor implements PriorityOrdered {

    private int order = Ordered.HIGHEST_PRECEDENCE;

    @Pointcut(value = "execution(public * *(..))")
    public void anyPublicMethod() { }

    @Around("@annotation(currentTenant))")
    public Object around(final ProceedingJoinPoint joinPoint, final CurrentTenant currentTenant) throws Throwable {
        SpringTenantIdentifierResolver.setCurrentTenant(currentTenant.value());
        try {
            return joinPoint.proceed();
        } finally {
            SpringTenantIdentifierResolver.resetCurrentTenant();
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
