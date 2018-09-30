package com.elihullc.rwsplitter.jpa.hibernate;

/**
 * A {@link Runnable} that sets the tenant on the current thread before invocation and resets it after invocation
 */
public class TenantSettingRunnable implements Runnable {

    private final String tenantIdentifier;
    private final Runnable delegate;

    public TenantSettingRunnable(final String tenantIdentifier, final Runnable delegate) {
        this.tenantIdentifier = tenantIdentifier;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        SpringTenantIdentifierResolver.setCurrentTenant(this.tenantIdentifier);
        try {
            this.delegate.run();
        } finally {
            SpringTenantIdentifierResolver.resetCurrentTenant();
        }
    }
}
