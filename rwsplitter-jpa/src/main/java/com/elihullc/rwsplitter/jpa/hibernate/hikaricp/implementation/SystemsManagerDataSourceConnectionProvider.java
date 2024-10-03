package com.elihullc.rwsplitter.jpa.hibernate.hikaricp.implementation;

import java.io.Serial;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersRequest;
import software.amazon.awssdk.services.ssm.model.Parameter;

public class SystemsManagerDataSourceConnectionProvider extends PropertiesFileDataSourceConnectionProvider {

    @Serial
    private static final long serialVersionUID = -5607834028317853608L;

    public SystemsManagerDataSourceConnectionProvider(String tenantIdentifier) {
        super(tenantIdentifier);
    }

    @Override
    protected void beforeConfiguration(final String tenantIdentifier) {
        final List<Parameter> parameters;
        try (SsmClient ssm = SsmClient.builder().region(Region.of(
          Optional.ofNullable(System.getenv("AWS_REGION")).orElse(System.getProperty("aws.region", "us-east-1"))
        )).build()) {

            this.properties = new Properties();
            parameters = ssm.getParameters(GetParametersRequest.builder()
              .withDecryption(true)
              .names(
                tenantIdentifier + ".database.url",
                tenantIdentifier + ".database.user",
                tenantIdentifier + ".database.password",
                tenantIdentifier + ".database.dataSourceClassName",
                tenantIdentifier + ".database.readOnly"
              ).build()).parameters();
            parameters.forEach(p -> {
                String name = p.name();
                name = name.substring(name.indexOf('.') + 1);
                this.properties.setProperty(name, p.value());
            });
        }
    }
}
