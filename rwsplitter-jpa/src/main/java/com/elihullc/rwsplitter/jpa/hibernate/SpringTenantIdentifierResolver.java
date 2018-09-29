package com.elihullc.rwsplitter.jpa.hibernate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Class implementing {@link CurrentTenantIdentifierResolver} that returns the tenant for the current thread.  First the
 * thread-local value of {@link #getCurrentTenant()} is used, if present.  Second, an attempt is made to retrieve the tenant
 * from the current {@link javax.servlet.http.HttpSession} under the session attribute {@link #TENANT_IDENTIFIER_ATTR}.  Finally,
 * if both of the previous values are null the value of {@link #getDefaultTenant()} is used.
 */
public class SpringTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    public static final String DEFAULT_TENANT = "master";

    private static final InheritableThreadLocal<List<String>> CURRENT_TENANT = new InheritableThreadLocal<>();
    private static final String TENANT_IDENTIFIER_ATTR = "tenantIdentifier";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private String defaultTenant = DEFAULT_TENANT;
    private String tenantIdentifierAttribute = TENANT_IDENTIFIER_ATTR;

    /**
     * {@inheritDoc}
     */
    @Override
    public String resolveCurrentTenantIdentifier() {
        final String tenantId = Optional.ofNullable(getCurrentTenant())
          .orElseGet(() -> Optional.ofNullable(RequestContextHolder.getRequestAttributes())
            .map(ServletRequestAttributes.class::cast)
            .map(ServletRequestAttributes::getRequest)
            .map(req -> req.getSession(false))
            .map(session -> session.getAttribute(getTenantIdentifierAttribute()))
            .map(Object::toString)
            .orElse(getDefaultTenant()));
        this.logger.trace("Resolved current tenant identifier to {}", tenantId);
        return tenantId;
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
     * Gets the value of the tenant attribute stored in a {@link javax.servlet.http.HttpSession}. Default is "tenantIdentifier"
     * @return the value of the tenant attribute stored in a {@link javax.servlet.http.HttpSession}
     */
    protected String getTenantIdentifierAttribute() {
        return this.tenantIdentifierAttribute;
    }

    /**
     * Sets the value of the tenant attribute stored in a {@link javax.servlet.http.HttpSession}
     * @param tenantIdentifierAttribute the value of the tenant attribute stored in a {@link javax.servlet.http.HttpSession}
     */
    public void setTenantIdentifierAttribute(final String tenantIdentifierAttribute) {
        this.tenantIdentifierAttribute = tenantIdentifierAttribute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
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
}
