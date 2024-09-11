package com.mycompany.app;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Configuration
public class DatasourceConfiguration {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    private final PasswordFetcher passwordFetcher;

    public DatasourceConfiguration(PasswordFetcher passwordFetcher) {
        this.passwordFetcher = passwordFetcher;
    }

    @Bean
    public HikariConfig config(@Value("${spring.datasource.url}")String dbUrl,@Value("${spring.datasource.username}")String dbUsername, PasswordFetcher passwordFetcher) throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUsername);
        config.setPassword(passwordFetcher.fetch());
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(10);
        config.setMaxLifetime(0);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setValidationTimeout(5000);
        config.setConnectionTestQuery("SELECT 1");
        return config;
    }

    @Bean
    public DataSource dataSource(HikariConfig config,PasswordFetcher passwordFetcher) throws Exception {
        return new CustomHikariDataSource(config,passwordFetcher);
    }

    private static class CustomHikariDataSource extends HikariDataSource {

        private int failedLoginAttempts = 0;
        private PasswordFetcher passwordFetcher;

        public CustomHikariDataSource(HikariConfig config,PasswordFetcher passwordFetcher) {
            super(config);
            this.passwordFetcher = passwordFetcher;
        }

        @Override
        public Connection getConnection() throws SQLException {
            try {
                return super.getConnection();
            } catch (SQLException e) {
                failedLoginAttempts++;
                if (failedLoginAttempts >= 3) {
                    lockoutUser();
                } else {
                    try {
                        refreshPassword();
                    } catch (Exception e1) {
                        throw new SQLException("", e1);
                    }
                }
                throw e;
            }
        }

        private void refreshPassword() throws Exception {
            this.setPassword(passwordFetcher.fetch());
            this.failedLoginAttempts = 0;
        }

        private void lockoutUser() {
            // Implement user lockout logic here
            log.error("User account locked out after 3 failed login attempts.");
        }
    }
}