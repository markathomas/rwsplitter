package com.elihullc.rwsplitter.jpa.hibernate.hikaricp.implementation;

import com.elihullc.rwsplitter.jpa.hibernate.SpringMultiTenantConnectionProvider;
import com.elihullc.rwsplitter.jpa.hibernate.SpringTenantIdentifierResolver;

import java.util.Map;
import java.util.function.Supplier;

public class PropertiesFileMultiTenantConnectionProvider
  extends SpringMultiTenantConnectionProvider<PropertiesFileDataSourceConnectionProvider> {

    private static final long serialVersionUID = 7706020243181333623L;

    public PropertiesFileMultiTenantConnectionProvider(final SpringTenantIdentifierResolver tenantIdentifierResolver) {
        super(tenantIdentifierResolver);
    }

    public PropertiesFileMultiTenantConnectionProvider(SpringTenantIdentifierResolver tenantIdentifierResolver,
      Supplier<Map<String, Boolean>> mapSupplier) {
        super(tenantIdentifierResolver, mapSupplier);
    }

    public PropertiesFileMultiTenantConnectionProvider(SpringTenantIdentifierResolver tenantIdentifierResolver,
      Map<String, Boolean> migratedTenants) {
        super(tenantIdentifierResolver, migratedTenants);
    }

    @Override
    protected PropertiesFileDataSourceConnectionProvider createMasterConnectionProvider(final String tenantIdentifier) {
        return new PropertiesFileDataSourceConnectionProvider(tenantIdentifier);
    }

    @Override
    protected PropertiesFileDataSourceConnectionProvider createReaderConnectionProvider(final String tenantIdentifier) {
        return new PropertiesFileDataSourceConnectionProvider(tenantIdentifier);
    }
}
