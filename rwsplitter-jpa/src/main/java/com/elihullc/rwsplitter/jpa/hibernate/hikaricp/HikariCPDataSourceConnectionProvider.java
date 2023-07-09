package com.elihullc.rwsplitter.jpa.hibernate.hikaricp;

import com.elihullc.rwsplitter.jpa.hibernate.StoppableConnectionProvider;
import com.zaxxer.hikari.hibernate.HikariConfigurationUtil;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;

/**
 * Abstract class implementing {@link StoppableConnectionProvider} that configures a {@link HikariCPConnectionProvider} using a
 * {@link javax.sql.DataSource} class name
 */
public abstract class HikariCPDataSourceConnectionProvider extends HikariCPConnectionProvider
  implements StoppableConnectionProvider {

    protected HikariCPDataSourceConnectionProvider(final String tenantIdentifier) {
        this.beforeConfiguration(tenantIdentifier);
        final Map<String, Object> dbProps = this.getDataSourceProperties(tenantIdentifier);
        this.configure(dbProps);
        this.afterConfiguration(tenantIdentifier);
    }

    /**
     * Populates and returns a map of properties used to configure a {@link HikariCPConnectionProvider} for the provided tenant
     * @param tenantIdentifier the tenant identifier
     * @return map of properties used to configure a {@link HikariCPConnectionProvider} for the provided tenant
     */
    protected Map<String, Object> getDataSourceProperties(String tenantIdentifier) {
        final Map<String, Object> props = new HashMap<>();
        props.put(AvailableSettings.AUTOCOMMIT, this.isReadOnly() ? "true" : "false");
        final String pre = HikariConfigurationUtil.CONFIG_PREFIX;
        props.put(pre + "dataSourceClassName", getDataSourceClassName());
        props.put(pre + "dataSource.url", getDatabaseURL());
        props.put(AvailableSettings.URL, props.get(pre + "dataSource.url"));
        props.put(pre + "dataSource.user", getDatabaseUser());
        props.put(AvailableSettings.USER, getDatabaseUser());
        props.put(pre + "dataSource.password", getDatabasePassword());
        props.put(AvailableSettings.PASS, getDatabasePassword());
        props.put(pre + "transactionIsolation", getIsolationLevel());
        props.put(pre + "poolName", getDataSourceName(tenantIdentifier));
        props.put(pre + "maximumPoolSize", String.valueOf(getMaximumPoolSize()));
        props.put(pre + "minimumIdle", String.valueOf(getMinimumPoolSize()));
        if (this.isReadOnly()) {
            props.put(pre + "autoCommit", "true");
            props.put(pre + "readOnly", "true");
        }
        props.put(pre + "registerMbeans", "true");
        return props;
    }

    /**
     * Returns a distinct name for the configured data source.  The default implementation returns the string consisting of the
     * tenant identifier + "Reader/Master Data Source" depending on the value of {@link #isReadOnly()}
     * @param tenantIdentifier the tenant identifier
     * @return a distinct name for the configured data source
     */
    protected String getDataSourceName(final String tenantIdentifier) {
        return tenantIdentifier + " " + (this.isReadOnly() ? "Reader" : "Master") + " Data Source";
    }

    /**
     * Returns the minimum pool size. Default is 1.
     * @return the minimum pool size
     */
    protected int getMinimumPoolSize() {
        return 1;
    }

    /**
     * Returns the maximum pool size. Default is 10.
     * @return the maximum pool size
     */
    protected int getMaximumPoolSize() {
        return 10;
    }

    private String getIsolationLevel() {
        return this.isReadOnly() ? getReaderIsolationLevel() : getWriterIsolationLevel();
    }

    /**
     * Returns the isolation level for writers.  Default is TRANSACTION_REPEATABLE_READ.
     * @return the isolation level for writers
     */
    protected String getWriterIsolationLevel() {
        return "TRANSACTION_REPEATABLE_READ";
    }

    /**
     * Returns the isolation level for readers.  Default is TRANSACTION_READ_COMMITTED.
     * @return the isolation level for readers
     */
    protected String getReaderIsolationLevel() {
        return "TRANSACTION_READ_COMMITTED";
    }

    /**
     * Returns the database password
     * @return the database password
     */
    protected abstract String getDatabasePassword();

    /**
     * Returns the database user
     * @return the database user
     */
    protected abstract String getDatabaseUser();

    /**
     * Returns the database URL
     * @return the database URL
     */
    protected abstract String getDatabaseURL();

    /**
     * Returns the data source class name
     * @return the data source class name
     */
    protected abstract String getDataSourceClassName();

    /**
     * Method invoked after the connection provider has been configured.  Useful for any necessary cleanup
     * @param tenantIdentifier the tenant identifier
     */
    protected void afterConfiguration(final String tenantIdentifier) {
    }

    /**
     * Method invoked before the connection provider is configured.  Useful for fetching anything necessary for setting properties
     * in {@link #getDataSourceProperties(String)}
     * @param tenantIdentifier the tenant identifier
     */
    protected void beforeConfiguration(final String tenantIdentifier) {
    }
}
