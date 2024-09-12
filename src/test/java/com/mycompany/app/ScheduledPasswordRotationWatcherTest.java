package com.mycompany.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class ScheduledPasswordRotationWatcherTest {
 
    @Mock
    private PasswordFetcher passwordFetcher;

    @Mock
    private Cryptor cryptor;

    @Mock
    private RestartEndpoint restartEndpoint;

    @Mock
    private DataSource dataSource;

    @Mock
    private Environment environment;

    @InjectMocks
    private ScheduledPasswordRotationWatcher watcher;

    private HikariDataSource hikariDataSource;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${logging.file.name}")
    private String logFileName;

    @Value("${passwordRotationWatcher.cron}")
    private String cronExpression;

    @Value("${spring.devtools.restart.enabled}")
    private boolean schedulerEnabled;

    @Value("${spring.cloud.config.enabled}")
    private String cloudConfigEnabled;

    @Value("${management.endpoints.web.exposure.include}")
    private String actuatorEndpoints;

    @BeforeEach
    public void setUp() throws Exception {
        Dotenv dotenv = Dotenv.load();
        when(passwordFetcher.fetch()).thenReturn(dotenv.get("DATASOURCE_PASSWORD"));
        when(cryptor.encode(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        hikariDataSource = new HikariDataSource();
        hikariDataSource.setJdbcUrl(dbUrl);
        hikariDataSource.setUsername(dbUsername);
        hikariDataSource.setPassword(dbPassword);
        hikariDataSource.setDriverClassName("org.h2.Driver");
        hikariDataSource.setMaximumPoolSize(10);
        hikariDataSource.setMaxLifetime(0); // Infinite idle time
        hikariDataSource.setConnectionTimeout(30000);
        hikariDataSource.setIdleTimeout(600000);
        hikariDataSource.setValidationTimeout(5000);
        hikariDataSource.setConnectionTestQuery("SELECT 1");

        when(dataSource.getConnection()).thenReturn(hikariDataSource.getConnection());
    }

    @Test
    public void testPasswordRotationDetected() throws Exception {
        when(passwordFetcher.fetch()).thenReturn("newpassword");
        watcher.execute();
        verify(restartEndpoint, times(1)).restart();
    }

    @Test
    public void testPasswordRotationNotDetected() throws Exception {
        when(passwordFetcher.fetch()).thenReturn("initialpassword");
        watcher.execute();
        verify(restartEndpoint, never()).restart();
    }

    @Test
    public void testHikariConnectionPoolInfiniteIdle() {
        assertEquals(0, hikariDataSource.getMaxLifetime());
    }

    @Test
    public void testDataSourceConnected() throws SQLException {
        try (Connection connection = hikariDataSource.getConnection()) {
            assertNotNull(connection);
        }
    }

    @Test
    public void testLoggingConfigurationApplied() {
        assertNotNull(logFileName);
        assertEquals("/workspaces/vscode-remote-try-java/logs/sandbox.log", logFileName);
    }

    @Test
    public void testPasswordRotationWatcherCron() {
        assertNotNull(cronExpression);
        assertEquals("15/30 * * * * ?", cronExpression);
    }

    @Test
    public void testSchedulerEnabled() {
        assertEquals(true, schedulerEnabled);
    }

    @Test
    public void testCloudConfigAndActuatorIncluded() {
        assertNotNull(cloudConfigEnabled);
        assertNotNull(actuatorEndpoints);
    }

    @Test
    public void testActuatorRestartEnabled() {
        assertNotNull(actuatorEndpoints);
        assertEquals("restart", actuatorEndpoints);
    }
}