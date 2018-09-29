### RWSplitter

RWSplitter allows you to implement single or multi-tenant read/write splitting using Spring and Hibernate without changes to your
existing Spring transactions.  Using the provided aspect, `DatabaseRoleInterceptor`, all public, read-only transactions will be
routed to your configured reader `DataSource`(s) and everything else will be routed to your configured writer `DataSource`(s).

Multi-tenant is achieved in one of two ways:
* Programmtically by setting the tenant identifier on the current thread using the static method
`SpringTenantIdentifierResolver.setCurrentTenant(String tenant)`, or
* Setting the tenant in your `HttpSession` under the session attribute `tenantIdentifier` (configurable, see below)

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
    public CurrentTenantIdentifierResolver tenantIdentifier() {
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

    @Bean
    public BeanPostProcessor persistenceExceptionTranslationPostProcessor() {
        return new PersistenceExceptionTranslationPostProcessor();
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
    
    // Sets the name of the HttpSession attribute used to lookup the current tenant if not programmatically set. Default is
    // "tenantIdentifier".
    resolver.setTenantIdentifierAttribute("myTenantAttribute");
    
    return resolver;
}

@Bean
public DatabaseRoleInterceptor databaseRoleInterceptor() {
    final DatabaseRoleInterceptor interceptor = new DatabaseRoleInterceptor();
    
    // Defines the sort order for the aspect. Lower values have higher priority. Default value is Integer.MIN_VALUE which is
    // equivalent to Ordered.HIGHEST_PRECEDENCE.  This is used to ensure this aspect is woven after Spring's @Transactional so that
    // it's code is invoked before @Transactional's
    interceptor.setOrder(100);
    
    return interceptor;
}
``` 
