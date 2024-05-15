package uk.gov.hmcts.reform.ethos.ecm.consumer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class CosDBConfiguration {
    @Value("${etcos.spring.datasource.url}")
    private String url;
    @Value("${etcos.spring.datasource.username}")
    private String username;
    @Value("${etcos.spring.datasource.password}")
    private String password;

    @Bean(name = "etcosDataSource")
    public DataSource etcosDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(url)
                .username(username)
                .password(password)
                .build();
    }

    @Bean
    @ConfigurationProperties("etcos.spring.datasource")
    public DataSourceProperties etcosDataSourceProperties() {
        return new DataSourceProperties();
    }
}