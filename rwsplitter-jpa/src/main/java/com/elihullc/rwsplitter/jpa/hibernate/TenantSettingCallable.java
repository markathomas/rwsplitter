package com.elihullc.rwsplitter.jpa.hibernate;

import java.util.concurrent.Callable;

/**
 * A {@link Callable} that sets the tenant on the current thread before invocation and resets it after invocation
 */
public class TenantSettingCallable<T> implements Callable<T> {

    private final String tenantIdentifier;
    private final Callable<T> delegate;

    public TenantSettingCallable(final String tenantIdentifier, final Callable<T> delegate) {
        this.tenantIdentifier = tenantIdentifier;
        this.delegate = delegate;
    }

    @Override
    public T call() throws Exception {
        SpringTenantIdentifierResolver.setCurrentTenant(this.tenantIdentifier);
        try {
            return this.delegate.call();
        } finally {
            SpringTenantIdentifierResolver.resetCurrentTenant();
        }
    }
}
