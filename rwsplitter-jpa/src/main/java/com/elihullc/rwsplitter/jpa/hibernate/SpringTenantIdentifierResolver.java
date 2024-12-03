package com.elihullc.rwsplitter.jpa.hibernate;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class implementing {@link CurrentTenantIdentifierResolver} that returns the tenant for the current thread.  First the
 * thread-local value of {@link #getCurrentTenant()} is used, if present.  Second, an attempt is made to retrieve the tenant
 * from either the default or supplied {@link Supplier<String>}.  Finally, if both of the previous values are null the value
 * of {@link #getDefaultTenant()} is used.
 */
public class SpringTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String>, Serializable {

    @Serial
    private static final long serialVersionUID = -23L;

    public static final String DEFAULT_TENANT = "master";

    private static final ThreadLocal<List<String>> CURRENT_TENANT = new ThreadLocal<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String defaultTenant = DEFAULT_TENANT;
    private Supplier<String> tenantSupplier;

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolveCurrentTenantIdentifier() {
        final String tenantId = Optional.ofNullable(getCurrentTenant())
          .orElseGet(() -> Optional.ofNullable(
            Optional.ofNullable(this.tenantSupplier).orElse(this.instantiateDefaultTenantSupplier()).get())
              .orElse(getDefaultTenant()));
        this.logger.trace("Resolved current tenant identifier to {}", tenantId);
        return tenantId;
    }

    @SuppressWarnings("unchecked")
    private Supplier<String> instantiateDefaultTenantSupplier() {
        try {
            final Class<?> klass = Class.forName("com.elihullc.rwsplitter.jpa.hibernate.SpringMVCTenantSupplier");
            this.tenantSupplier = (Supplier<String>)klass.newInstance();
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            throw new RuntimeException("Error instantiating default tenant supplier", e);
        }
        return this.tenantSupplier;
    }

    /**
     * Returns the default tenant identifier.  Default is "master". Override to change.
     * @return the default tenant identifier
     */
    public String getDefaultTenant() {
        return this.defaultTenant;
    }

    /**
     * Sets the value for the default tenant.
     * @param defaultTenant the value for the default tenant
     */
    public void setDefaultTenant(final String defaultTenant) {
        this.defaultTenant = defaultTenant;
    }

    /**
     * Retrieves the current tenant supplier. Default is implementation of {@link SpringMVCTenantSupplier}
     * @return the current tenant supplier
     */
    public Supplier<String> getTenantSupplier() {
        return this.tenantSupplier;
    }

    /**
     * Sets the current tenant supplier
     * @param tenantSupplier the tenant supplier
     */
    public void setTenantSupplier(Supplier<String> tenantSupplier) {
        this.tenantSupplier = tenantSupplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    /**
     * Returns all tenants that are queued within this thread
     * @return all tenants that are queued within this thread
     */
    public static synchronized List<String> getAllTenants() {
        List<String> tenants = CURRENT_TENANT.get();
        if (tenants == null) {
            tenants = new ArrayList<>();
        } else {
            tenants = new ArrayList<>(tenants);
        }
        return tenants;
    }

    /**
     * Gets the value of the current tenant thread-local
     * @return the value of the current tenant thread-local
     */
    public synchronized String getCurrentTenant() {
        List<String> tenants = CURRENT_TENANT.get();
        if (tenants == null) {
            return null;
        }
        return tenants.get(tenants.size() - 1);
    }

    /**
     * Sets the value of the current tenant thread-local to the provided tenant identifier
     * @param tenantIdentifier the tenant identifier
     */
    public static synchronized void setCurrentTenant(final String tenantIdentifier) {
        List<String> tenants = CURRENT_TENANT.get();
        if (tenants == null) {
            tenants = new ArrayList<>();
        }
        tenants.add(tenantIdentifier);
        CURRENT_TENANT.set(tenants);
    }

    /**
     * Resets the current tenant thread-local value to the previous value or removes it if none exists
     */
    public static synchronized void resetCurrentTenant() {
        List<String> tenants = CURRENT_TENANT.get();
        if (tenants == null) {
            return;
        }
        tenants.remove(tenants.size() - 1);
        if (tenants.isEmpty()) {
            CURRENT_TENANT.remove();
        } else {
            CURRENT_TENANT.set(tenants);
        }
    }

    /**
     * Removes all tenants from current thread
     */
    public static synchronized void clearAllTenants() {
        CURRENT_TENANT.set(new ArrayList<>());
    }
}
