package com.elihullc.rwsplitter.jpa.hibernate.hikaricp.implementation;

import com.elihullc.rwsplitter.jpa.hibernate.SpringMultiTenantConnectionProvider;
import com.elihullc.rwsplitter.jpa.hibernate.SpringTenantIdentifierResolver;

import java.io.Serial;
import java.util.Map;
import java.util.function.Supplier;

public class SystemsManagerMultiTenantConnectionProvider
  extends SpringMultiTenantConnectionProvider<SystemsManagerDataSourceConnectionProvider> {

    @Serial
    private static final long serialVersionUID = 1578118147882724511L;

    public SystemsManagerMultiTenantConnectionProvider(SpringTenantIdentifierResolver tenantIdentifierResolver) {
        super(tenantIdentifierResolver);
    }

    public SystemsManagerMultiTenantConnectionProvider(SpringTenantIdentifierResolver tenantIdentifierResolver,
      Supplier<Map<String, Boolean>> mapSupplier) {
        super(tenantIdentifierResolver, mapSupplier);
    }

    public SystemsManagerMultiTenantConnectionProvider(SpringTenantIdentifierResolver tenantIdentifierResolver,
      Map<String, Boolean> migratedTenants) {
        super(tenantIdentifierResolver, migratedTenants);
    }

    @Override
    protected SystemsManagerDataSourceConnectionProvider createMasterConnectionProvider(String tenantIdentifier) {
        return new SystemsManagerDataSourceConnectionProvider(tenantIdentifier);
    }

    @Override
    protected SystemsManagerDataSourceConnectionProvider createReaderConnectionProvider(String tenantIdentifier) {
        return new SystemsManagerDataSourceConnectionProvider(tenantIdentifier);
    }
}
