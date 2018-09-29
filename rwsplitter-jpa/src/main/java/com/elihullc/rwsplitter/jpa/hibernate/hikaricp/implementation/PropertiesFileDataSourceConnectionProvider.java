package com.elihullc.rwsplitter.jpa.hibernate.hikaricp.implementation;

import com.elihullc.rwsplitter.jpa.hibernate.hikaricp.HikariCPDataSourceConnectionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesFileDataSourceConnectionProvider extends HikariCPDataSourceConnectionProvider {

    private static final long serialVersionUID = 4473698965793267295L;

    private Properties properties = new Properties();

    public PropertiesFileDataSourceConnectionProvider(final String tenantIdentifier) {
        super(tenantIdentifier);
    }

    @Override
    protected void beforeConfiguration(final String tenantIdentifier) {
        super.beforeConfiguration(tenantIdentifier);
        this.properties = new Properties();
        try (InputStream is = this.getClass().getResourceAsStream("/" + tenantIdentifier + ".properties")) {
            this.properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String getDatabasePassword() {
        return this.properties.getProperty("database.password");
    }

    @Override
    protected String getDatabaseUser() {
        return this.properties.getProperty("database.user");
    }

    @Override
    protected String getDatabaseURL() {
        return this.properties.getProperty("database.url");
    }

    @Override
    protected String getDataSourceClassName() {
        return this.properties.getProperty("database.dataSourceClassName");
    }

    @Override
    public boolean isReadOnly() {
        return Boolean.valueOf(this.properties.getProperty("database.readOnly"));
    }
}
