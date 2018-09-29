### RWSplitter

RWSplitter allows you to implement single or multi-tenant read/write splitting using Spring and Hibernate without changes to your
existing Spring transactions.  Using the provided aspect, `DatabaseRoleInterceptor`, all public, read-only transactions will be routed
to your configured reader `DataSource`(s) and everything else will be routed to your configured writer `DataSource`(s).

Multi-tenant is achieved in one of two ways:
* Programmtically by setting the tenant identifier on the current thread using the static method
`SpringTenantIdentifierResolver.setCurrentTenant(String tenant)`, or
* Setting the tenant in your `HttpSession` under the session attribute `tenantIdentifier` (configurable)

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

