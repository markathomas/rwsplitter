1.0.2 - released 2020-01-23

* Loosened method visibility of `SpringMultiTenantConnectionProvider#getConnectionProvider` to public
* Added more constructors to `PropertiesFileDataSourceConnectionProvider`
* Made Spring MVC optional by extracting previously required session variable to define tenant into a `Supplier<String>`.  A Spring MVC implementation is available as the default for backwards compatiblity
* Added example of using Amazon AWS SSM Parameter Store for storing DB secrets
 
1.0.1 - released 2018-10-16

* Fixed default order for aspects
* Loosened method visibility of `SpringMultiTenantConnectionProvider#getConnectionProvider` to protected
