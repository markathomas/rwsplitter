package com.elihullc.rwsplitter.jpa.hibernate.hikaricp.implementation;

import com.elihullc.rwsplitter.jpa.hibernate.SpringMultiTenantConnectionProvider;
import com.elihullc.rwsplitter.jpa.hibernate.SpringTenantIdentifierResolver;

import java.io.Serial;

public class SystemsManagerMultiTenantConnectionProvider
  extends SpringMultiTenantConnectionProvider<SystemsManagerDataSourceConnectionProvider> {

    @Serial
    private static final long serialVersionUID = 1578118147882724511L;

    public SystemsManagerMultiTenantConnectionProvider(SpringTenantIdentifierResolver tenantIdentifierResolver) {
        super(tenantIdentifierResolver);
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
