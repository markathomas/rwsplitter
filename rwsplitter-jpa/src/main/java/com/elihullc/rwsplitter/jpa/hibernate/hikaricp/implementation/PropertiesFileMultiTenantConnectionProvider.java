package com.elihullc.rwsplitter.jpa.hibernate.hikaricp.implementation;

import com.elihullc.rwsplitter.jpa.hibernate.SpringMultiTenantConnectionProvider;
import com.elihullc.rwsplitter.jpa.hibernate.SpringTenantIdentifierResolver;

public class PropertiesFileMultiTenantConnectionProvider
  extends SpringMultiTenantConnectionProvider<PropertiesFileDataSourceConnectionProvider> {

    public PropertiesFileMultiTenantConnectionProvider(final SpringTenantIdentifierResolver tenantIdentifierResolver) {
        super(tenantIdentifierResolver);
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
