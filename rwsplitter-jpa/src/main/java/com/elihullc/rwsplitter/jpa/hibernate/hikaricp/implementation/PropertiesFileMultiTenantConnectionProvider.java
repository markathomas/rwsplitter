package com.elihullc.rwsplitter.jpa.hibernate.hikaricp.implementation;

import com.elihullc.rwsplitter.jpa.hibernate.SpringMultiTenantConnectionProvider;
import com.elihullc.rwsplitter.jpa.hibernate.SpringTenantIdentifierResolver;

public class PropertiesFileMultiTenantConnectionProvider
  extends SpringMultiTenantConnectionProvider<PropertiesFileDataSourceConnectionProvider> {

    private static final long serialVersionUID = 7706020243181333623L;
    
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
