package com.elihullc.rwsplitter.jpa.hibernate;

import com.elihullc.rwsplitter.jpa.CurrentDatabaseRole;
import com.elihullc.rwsplitter.jpa.DatabaseRole;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.engine.jdbc.connections.spi.AbstractMultiTenantConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Support for {@link MultiTenantConnectionProvider} implementations using individual {@link ConnectionProvider} instances per
 * tenant behind the scenes.
 *
 * Support is also provided to use multi-tenant only for specific tenants via setting either the
 * environment variable MIGRATED_TENANTS or the system property migrated.tenants to a CSV or tenant identifiers.  If present, all
 * tenants specified will use a distinct {@link ConnectionProvider}; otherwise, if a requested tenant is absent from this value it
 * will use the tenant specified by {@link SpringTenantIdentifierResolver#getDefaultTenant()}.  This is useful during migrations
 * from a single database hosting multiple tenants into multiple databases/schemas so that migrations can be done incrementally.
 *
 * Additionally, this class supports read/write splitting by checking {@link CurrentDatabaseRole#getCurrentRole()}'s thread-local
 * value or the value returned by {@link TransactionSynchronizationManager#isCurrentTransactionReadOnly()}.
 *
 * @author Mark Thomas
 */
@ManagedResource
public abstract class SpringMultiTenantConnectionProvider<T extends StoppableConnectionProvider>
  extends AbstractMultiTenantConnectionProvider<String> implements Closeable {

    private final ConcurrentHashMap<String, T> connectionProviders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, T> readOnlyConnectionProviders = new ConcurrentHashMap<>();

    private final transient SpringTenantIdentifierResolver tenantIdentifierResolver;
    private final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected SpringMultiTenantConnectionProvider(final SpringTenantIdentifierResolver tenantIdentifierResolver) {
        this.tenantIdentifierResolver = tenantIdentifierResolver;
    }

    /**
     * Clears and closes all connection providers. Useful fo requiring a re-initialization of connection providers after a specific
     * event requires it (e.g. Failover ov writer to a read-replica)
     */
    @ManagedOperation(description = "Clears and closes all connection providers")
    public void clearAll() {
        this.close();
        this.connectionProviders.clear();
        this.readOnlyConnectionProviders.clear();
    }

    /**
     * @inheritDoc
     */
    @Override
    protected ConnectionProvider getAnyConnectionProvider() {
        final String tenant = this.tenantIdentifierResolver.resolveCurrentTenantIdentifier();
        this.logger.trace("Selecting any/default connection provider of {}", tenant);
        return this.selectConnectionProvider(tenant);
    }

    /**
     * Returns a {@link StoppableConnectionProvider} depending on the current read-only definition as defined by
     * {@link CurrentDatabaseRole#getCurrentRole()}'s thread-local value or the value returned by
     * {@link TransactionSynchronizationManager#isCurrentTransactionReadOnly()}
     * @param tenantIdentifier the tenant provider
     * @return a {@link StoppableConnectionProvider}
     */
    @Override
    protected ConnectionProvider selectConnectionProvider(String tenantIdentifier) {
        this.logger.trace("Selecting specific connection provider for tenant {}", tenantIdentifier);
        return this.getConnectionProvider(tenantIdentifier);
    }

    public StoppableConnectionProvider getConnectionProvider(final String tenantIdentifier) {
        if (CurrentDatabaseRole.getCurrentRole() == DatabaseRole.READER
          || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            this.logger.trace("Fetching reader connection provider for tenant {}", tenantIdentifier);
            return this.getOrCreateReaderConnectionProvider(tenantIdentifier);
        }
        this.logger.trace("Fetching writer connection provider for tenant {}", tenantIdentifier);
        return this.getOrCreateMasterConnectionProvider(tenantIdentifier);
    }

    /**
     * Retrieves a {@link StoppableConnectionProvider} for the specified tenant and, if null, creates a new one
     * @param tenantIdentifier then tenant identifier
     * @return a {@link StoppableConnectionProvider} for the specified tenant
     */
    protected T getOrCreateMasterConnectionProvider(final String tenantIdentifier) {
        return this.connectionProviders.computeIfAbsent(tenantIdentifier, this::createMasterConnectionProvider);
    }

    /**
     * Retrieves a {@link StoppableConnectionProvider} for the specified tenant and, if null, creates a new one
     * @param tenantIdentifier then tenant identifier
     * @return a {@link StoppableConnectionProvider} for the specified tenant
     */
    protected T getOrCreateReaderConnectionProvider(final String tenantIdentifier) {
        return this.readOnlyConnectionProviders.computeIfAbsent(tenantIdentifier, this::createReaderConnectionProvider);
    }

    /**
     * Returns a new instance of {@link StoppableConnectionProvider} for the given tenant identifier
     * @param tenantIdentifier then tenant identifier
     * @return a new instance of {@link StoppableConnectionProvider} for the given tenant identifier
     */
    protected abstract T createMasterConnectionProvider(final String tenantIdentifier);

    /**
     * Returns a new instance of {@link StoppableConnectionProvider} for the given tenant identifier
     * @param tenantIdentifier then tenant identifier
     * @return a new instance of {@link StoppableConnectionProvider} for the given tenant identifier
     */
    protected abstract T createReaderConnectionProvider(final String tenantIdentifier);

    /**
     * Closes all connection providers
     */
    @Override
    public void close() {
        this.connectionProviders.values().forEach(StoppableConnectionProvider::stop);
        this.readOnlyConnectionProviders.values().forEach(StoppableConnectionProvider::stop);
    }
}
