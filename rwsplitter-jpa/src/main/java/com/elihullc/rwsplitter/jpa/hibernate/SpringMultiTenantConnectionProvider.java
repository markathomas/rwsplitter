package com.elihullc.rwsplitter.jpa.hibernate;

import com.elihullc.rwsplitter.jpa.CurrentDatabaseRole;
import com.elihullc.rwsplitter.jpa.DatabaseRole;

import java.io.Closeable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.sql.DataSource;

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
  extends AbstractMultiTenantConnectionProvider implements Closeable {

    private final ConcurrentHashMap<String, T> connectionProviders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, T> readOnlyConnectionProviders = new ConcurrentHashMap<>();

    private final transient SpringTenantIdentifierResolver tenantIdentifierResolver;
    private final transient Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, Boolean> migratedTenants;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    protected SpringMultiTenantConnectionProvider(final SpringTenantIdentifierResolver tenantIdentifierResolver) {
        this(tenantIdentifierResolver, new ConcurrentHashMap<>());
    }
    protected SpringMultiTenantConnectionProvider(final SpringTenantIdentifierResolver tenantIdentifierResolver,
      final Supplier<Map<String, Boolean>> mapSupplier) {
        this(tenantIdentifierResolver, mapSupplier.get());
    }
    protected SpringMultiTenantConnectionProvider(final SpringTenantIdentifierResolver tenantIdentifierResolver,
      final Map<String, Boolean> migratedTenants) {
        this.tenantIdentifierResolver = tenantIdentifierResolver;
        this.migratedTenants = migratedTenants;
    }

    /**
     * Returns a {@link Map} of tenants that have their own database or schema and require their own {@link DataSource}
     * @return {@link Map} of tenants that have their own database or schema and require their own {@link DataSource}
     */
    protected Map<String, Boolean> getMigratedTenants() {
        if (this.initialized.compareAndSet(false, true)) {
            final String migrated = Optional.ofNullable(System.getenv("MIGRATED_TENANTS"))
              .orElse(System.getProperty("migrated.tenants", ""));
            if (!migrated.isEmpty()) {
                Stream.of(migrated.split(",")).map(String::trim).forEach(tenant ->
                  this.migratedTenants.put(tenant, Boolean.TRUE));
            }
        }
        return this.migratedTenants;
    }

    /**
     * Adds a tenant to the map of migrated tenants via their tenant identifier
     * @param tenantIdentifier the tenant identifier
     * @return previous value of map if tenant was present, false otherwise
     */
    @ManagedOperation(description = "Adds a tenant to the map of migrated tenants via their tenant identifier")
    public boolean addMigratedTenant(String tenantIdentifier) {
        return Optional.ofNullable(this.getMigratedTenants().put(tenantIdentifier, Boolean.TRUE)).orElse(false);
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
        this.logger.trace("Selecting any/default connection provider of {}", this.tenantIdentifierResolver.getDefaultTenant());
        return this.selectConnectionProvider(this.tenantIdentifierResolver.getDefaultTenant());
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
        if (!Objects.equals(tenantIdentifier, this.tenantIdentifierResolver.getDefaultTenant())
          && !this.getMigratedTenants().isEmpty()
          && !this.getMigratedTenants().getOrDefault(tenantIdentifier, Boolean.FALSE)) {
            this.logger.error("Attempting to use a schema/database that does not yet exist for tenant {}.  Please migrate the "
              + "schema/database and then invoke this bean's JMX addMigratedTenant(String tenantIdentifier) "
              + "operation to signal completion", tenantIdentifier);
            tenantIdentifier = this.tenantIdentifierResolver.getDefaultTenant();
        }
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
