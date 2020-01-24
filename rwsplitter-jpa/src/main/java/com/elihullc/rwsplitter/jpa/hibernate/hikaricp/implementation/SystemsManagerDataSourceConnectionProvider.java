package com.elihullc.rwsplitter.jpa.hibernate.hikaricp.implementation;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParametersRequest;
import com.amazonaws.services.simplesystemsmanagement.model.Parameter;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class SystemsManagerDataSourceConnectionProvider extends PropertiesFileDataSourceConnectionProvider {

    private static final long serialVersionUID = -5607834028317853608L;

    public SystemsManagerDataSourceConnectionProvider(String tenantIdentifier) {
        super(tenantIdentifier);
    }

    @Override
    protected void beforeConfiguration(final String tenantIdentifier) {
        final AWSSimpleSystemsManagement ssm = AWSSimpleSystemsManagementClientBuilder.standard().withRegion(
          Optional.ofNullable(System.getenv("AWS_REGION")).orElse(System.getProperty("aws.region", "us-east-1"))
        ).build();

        this.properties = new Properties();
        final List<Parameter> parameters = ssm.getParameters(new GetParametersRequest().withWithDecryption(true)
          .withNames(
            tenantIdentifier + ".database.url",
            tenantIdentifier + ".database.user",
            tenantIdentifier + ".database.password",
            tenantIdentifier + ".database.dataSourceClassName",
            tenantIdentifier + ".database.readOnly"
          )).getParameters();
        parameters.forEach(p -> {
            String name = p.getName();
            name = name.substring(name.indexOf('.') + 1);
            this.properties.setProperty(name, p.getValue());
        });
    }
}
