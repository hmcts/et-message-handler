package uk.gov.hmcts.reform.ethos.ecm.consumer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class CosDataSource {
    @ConfigurationProperties("etcos.spring.datasource")
    public DataSource etcos() {
        return DataSourceBuilder.create().build();
    }
}
