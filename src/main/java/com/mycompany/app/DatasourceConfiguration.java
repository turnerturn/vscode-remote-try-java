package com.mycompany.app;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableConfigurationProperties(DatasourceProperties.class)
public class DatasourceConfiguration {


    @Bean
    public DataSource dataSource(DatasourceProperties datasourceProperties, PasswordFetcher passwordFetcher) throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(datasourceProperties.getUrl());
        config.setUsername(datasourceProperties.getUsername());
        config.setPassword(passwordFetcher.fetch());
        config.setDriverClassName(datasourceProperties.getDriverClassName());
        config.setMaximumPoolSize(datasourceProperties.getMaximumPoolSize());
        config.setMaxLifetime(datasourceProperties.getMaxLifetime());
        config.setConnectionTimeout(datasourceProperties.getConnectionTimeout());
        config.setIdleTimeout(datasourceProperties.getIdleTimeout());
        config.setValidationTimeout(datasourceProperties.getValidationTimeout());
        config.setConnectionTestQuery(datasourceProperties.getConnectionTestQuery());
        return new HikariDataSource(config);
    }
}

interface PasswordFetcher {
    public String fetch()throws Exception ;
}

@Slf4j
@Profile("cyberark")
@Component
class CyberarkPasswordFetcher implements PasswordFetcher{

    //TODO fetch this via cyberark cli
    public String fetch() throws Exception {
        log.info("fetch()");
        try { 
             Dotenv dotenv = Dotenv.configure().directory(System.getProperty("user.home")).ignoreIfMissing().load();
            return dotenv.get("DATASOURCE_PASSWORD");
   
        }catch(Exception e){
            log.error("Error fetching password", e);
            throw e;
        }
    }
}
@Slf4j
@Profile("!cyberark")
@Component
class LocalPasswordFetcher implements PasswordFetcher {

    public String fetch() throws Exception {
        log.info("fetch()");
        try { 
             Dotenv dotenv = Dotenv.configure().directory(System.getProperty("user.home")).ignoreIfMissing().load();
            return dotenv.get("DATASOURCE_PASSWORD");
   
        }catch(Exception e){
            log.error("Error fetching password", e);
            throw e;
        }
    }
}
