### RWSplitter

RWSplitter allows you to implement single or multi-tenant read/write splitting using Spring and Hibernate without changes to your
existing Spring transactions.  Using the provided aspect, `DatabaseRoleInterceptor`, all public, read-only transactions will be
routed to your configured reader `DataSource`(s) and everything else will be routed to your configured writer `DataSource`(s).

Multi-tenant is achieved in one of two ways:
* Programmtically by setting the tenant identifier on the current thread using the static method
`SpringTenantIdentifierResolver.setCurrentTenant(String tenant)`, or
* Setting the tenant in your `HttpSession` under the session attribute `tenantIdentifier` (configurable, see below)

There are two concrete implementations available in
this project.  The first is named `PropertiesFileMultiTenantConnectionProvider` that creates instances of
`PropertiesFileDataSourceConnectionProvider` which is based off of properties files in the classpath and.  To use this implementation
there should be a properties file for every available tenant in the root of your classpath named `[tenant identifier].properties`.
The second is `SystemsManagerMultiTenantConnectionProvider` that creates instances of `SystemsManagerDataSourceConnectionProvider` which reads from Amazon SSM Parameter Store for variables used to configure the connection pool. 
Additionally you must have `hibernate-hikaricp.jar` and an implementation of HikariCP on your classpath.  

#### Configuration using Spring JavaConfig

Assuming you have a concrete implementation of `SpringMultiTenantConnectionProvider` named `MySpringMultiTenantConnectionProvider`
your JavaConfig would look something like this:

```
import com.elihullc.rwsplitter.jpa.hibernate.SpringTenantIdentifierResolver;

@Configuration
@EnableAspectJAutoProxy
@ComponentScan("com.elihullc.rwsplitter.jpa.aop")
public class RWSplitterConfiguration {

    @Bean
    public SpringTenantIdentifierResolver tenantIdentifier() {
        return new SpringTenantIdentifierResolver();
    }

    @Bean
    public MultiTenantConnectionProvider multiTenantConnectionProvider() {
        return new MySpringMultiTenantConnectionProvider(tenantIdentifier());
    }
    
    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setDatabase(Database.MYSQL); // your DB vendor here
        vendorAdapter.setGenerateDdl(false);
        return vendorAdapter;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setJpaVendorAdapter(jpaVendorAdapter());
        factory.setPersistenceUnitName("default"); // your persistence unit name
        factory.setPackagesToScan("com.example.model"); // your model package
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.multi_tenant_connection_provider", multiTenantConnectionProvider());
        properties.put("hibernate.tenant_identifier_resolver", tenantIdentifier());
        properties.put("hibernate.multiTenancy", "DATABASE"); // DATABASE or SCHEMA here
        // other properties here
        factory.setJpaPropertyMap(properties);
        return factory;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory,
      JpaVendorAdapter vendorAdapter) {
        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager(entityManagerFactory);
        jpaTransactionManager.setJpaDialect(vendorAdapter.getJpaDialect());
        jpaTransactionManager.setJpaPropertyMap(entityManagerFactory.getProperties());
        return jpaTransactionManager;
    }

    @Bean
    public ValidatorFactory validatorFactory() {
        return new LocalValidatorFactoryBean();
    }
}
```

#### Customizing the Multi-Tenant Configuration

The following example shows the available properties to customize the multi-tenant configuration:

```
@Bean
public CurrentTenantIdentifierResolver tenantIdentifier() {
    final SpringTenantIdentifierResolver resolver = new SpringTenantIdentifierResolver();
    
    // Sets the default/fallback tenant if not explicitly set on the current thread or in the HttpSession. Default is "master".
    resolver.setDefaultTenant("myTenant"); 
    
    // Sets the Supplier from which to provide the tenant identifier. Defaults to SpringMVCTenantSupplier
    resolver.setTenantSupplier(() -> "my tenant from somewhere, e.g. (session, cache, etc.)"));
    
    return resolver;
}

@Bean
public DatabaseRoleInterceptor databaseRoleInterceptor() {
    final DatabaseRoleInterceptor interceptor = new DatabaseRoleInterceptor();
    
    // Defines the sort order for the aspect. Lower values have higher priority. Default value is 20 which is enough to
    // ensure this aspect is woven after Spring's @Transactional so that it's code is invoked before @Transactional's
    interceptor.setOrder(100);
    
    return interceptor;
}
``` 

#### Setting the Tenant via Spring AOP

Simply annotate your methods with the provided `@CurrentTenant` annotation and include `TenantSettingInterceptor` in your Spring
context or autowire `com.elihullc.rwsplitter.jpa.hibernate.aop` to have the tenant specified by `@CurrentTenant.value()` set before
method invocation and removed after invocation.

#### Common Pitfalls

If you're relying on an attribute in the `HttpSession` to specify the current tenant then by far the most common pitfall is
forgetting to programmatically set the current tenant inside an asynchronous method invoked via a new thread or `ExecutorService`.
To rectify simply set the current tenant inside your thread's `run()` method, `Runnable` or `Callable` or, use the helper classes
`TenantSettingRunnable` and `TenantSettingCallable` present in this project or use `@CurrentTenant`; 

The next most common pitfall is not setting the current tenant before your transaction is started.  

Another common pitfall is programmatically setting the current tenant and not resetting it in a `finally` block.  This will leave
the previous tenant set on the current thread and can lead to memory leaks.  ALWAYS set the current tenant inside a try/finally
block if setting programmatically! `TenantSettingRunnable` and `TenantSettingCallable` present in this project are again your
friends here or use `@CurrentTenant`. 

#### Installation

RWSplitter is available from [Maven Central](https://search.maven.org/#search|ga|1|a%3Arwsplitter-jpa):

Latest version:
```xml
    <dependency>
        <groupId>com.elihullc</groupId>
        <artifactId>rwsplitter-jpa</artifactId>
        <version>2.0.2</version>
    </dependency>
```

