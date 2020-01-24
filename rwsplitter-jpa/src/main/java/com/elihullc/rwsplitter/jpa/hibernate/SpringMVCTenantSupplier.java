package com.elihullc.rwsplitter.jpa.hibernate;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SpringMVCTenantSupplier implements Supplier<String> {

    private static final String TENANT_IDENTIFIER_ATTR = "tenantIdentifier";

    private String tenantIdentifierAttribute = TENANT_IDENTIFIER_ATTR;

    @Override
    public String get() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
          .map(ServletRequestAttributes.class::cast)
          .map(ServletRequestAttributes::getRequest)
          .map(req -> req.getSession(false))
          .map(session -> session.getAttribute(getTenantIdentifierAttribute()))
          .map(Object::toString)
          .orElse(null);
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
}
