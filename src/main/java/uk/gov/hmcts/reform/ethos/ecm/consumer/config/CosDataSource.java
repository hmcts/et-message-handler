package uk.gov.hmcts.reform.ethos.ecm.consumer.config;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CosDataSource {
  @ConfigurationProperties("etcos.spring.datasource")
  public DataSource etcos() {
    return DataSourceBuilder.create().build();
  }
}
