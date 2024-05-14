package uk.gov.hmcts.reform.ethos.ecm.consumer.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class CosDataSource {
    @Qualifier("etcos")
    @Bean
    @ConfigurationProperties("etcos.spring.datasource")
    public DataSource etcos() {
        return DataSourceBuilder.create().build();
    }
}
