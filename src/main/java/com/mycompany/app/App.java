package com.mycompany.app;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.UUID;

import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//import org.springframework.cloud.context.restart.RestartEndpoint;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}


@Slf4j
@Component
class Cryptor {

    private static final String ALGORITHM = "AES";

    private SecretKeySpec getKey(String secret) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = secret.getBytes("UTF-8");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // Use only the first 16 bytes (128 bits)
        return new SecretKeySpec(key, ALGORITHM);
    }

    public String encode(String data, String secret) throws Exception {
        SecretKeySpec key = getKey(secret);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decode(String encryptedData, String secret) throws Exception {
        try {
            SecretKeySpec key = getKey(secret);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decodedBytes);
            return new String(decrypted);
        } catch (javax.crypto.BadPaddingException e) {
            // Handle the exception gracefully
            throw new IllegalArgumentException("Invalid decryption key or corrupted data", e);
        }
    }
}

@Slf4j
@Configuration
@EnableConfigurationProperties(DatasourceProperties.class)
class DatasourceConfiguration {

    @Autowired
    private DatasourceProperties datasourceProperties;
    @Autowired
    private PasswordFetcher passwordFetcher;

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(datasourceProperties.getUrl());
        config.setUsername(datasourceProperties.getUsername());
        try{
            config.setPassword(passwordFetcher.fetch());
        }catch(Exception e){
        }
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

@Configuration
@ConfigurationProperties(prefix = "spring.datasource")
 class DatasourceProperties {

    private String url;
    private String username;
    private String driverClassName;
    private int maximumPoolSize;
    private long maxLifetime;
    private long connectionTimeout;
    private long idleTimeout;
    private long validationTimeout;
    private String connectionTestQuery;

    // Getters and setters

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public long getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public long getValidationTimeout() {
        return validationTimeout;
    }

    public void setValidationTimeout(long validationTimeout) {
        this.validationTimeout = validationTimeout;
    }

    public String getConnectionTestQuery() {
        return connectionTestQuery;
    }

    public void setConnectionTestQuery(String connectionTestQuery) {
        this.connectionTestQuery = connectionTestQuery;
    }
}


/**
 * Scheduled @hourly to compare cyberark agent's password to our cached value.
 * Cached value is encoded with our random uuid cryptor secret. When cached is
 * not null and encoded current password does not match our cached encoded value
 * then we detect password rotation.
 * Given password rotation detection: We execute our recovery command via the
 * ShellCommandExecutor. We will configure our command in the .yml to restart
 * the service via command line.
 * When service is restarted, we will have a fresh datasource containing our
 * latest cyberark password.
 */
@Slf4j
@Component
class ScheduledPasswordRotationWatcher {
  private static String cache;

  private static final String cryptorSecret = UUID.randomUUID().toString();

  private final PasswordFetcher passwordFetcher;
  private final Cryptor cryptor;

  private final RestartEndpoint restartEndpoint;
  public ScheduledPasswordRotationWatcher(PasswordFetcher passwordFetcher, Cryptor cryptor,
      final RestartEndpoint restartEndpoint) {
    this.passwordFetcher = passwordFetcher;
    this.cryptor = cryptor;
    this.restartEndpoint = restartEndpoint;
  }

  @Scheduled(cron = "${passwordRotationWatcher.cron}")
  public void execute() throws Exception {
    String encodedPassword = null;
    try {
      if (cache == null) {
        cache = cryptor.encode(passwordFetcher.fetch(), cryptorSecret);
      }

      encodedPassword = cryptor.encode(passwordFetcher.fetch(), cryptorSecret);
      log.info("{} vs {}",encodedPassword,cache);
      if (!encodedPassword.equalsIgnoreCase(cache)) {
        log.info("Password rotation detected. Restarting service.");
        restartEndpoint.restart();
      } else {
        log.info("Password rotation not detected.");
        return;
      }
    } finally {
       cache = encodedPassword;
    }
  }

}
