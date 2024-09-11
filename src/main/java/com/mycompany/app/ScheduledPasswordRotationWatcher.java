package com.mycompany.app;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
//import org.springframework.cloud.context.restart.RestartEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
//import jakarta.annotation.PostConstruct;
//import com.magellanlp.tas.eventpoller.common.ShellCommandExecutor;

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
public class ScheduledPasswordRotationWatcher {
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

  @Scheduled(initialDelay = 10000, fixedRate = 15000) // cron = "${passwordRotationWatcher.cron}")
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

@Slf4j
@Component
class ShellCommandExecutor {

  public String execute(String command) {
    log.trace("execute({})", command);
    StringBuilder sb = new StringBuilder();
    /*
     * 
     * 
     * String os = System.getProperty("os.name").toLowerCase();
     * if (os.contains("win")) {
     * command = "cmd /c " + command;
     * } else {
     * command = "/bin/bash -c " + command;
     * }
     */
    try {
      Process process = Runtime.getRuntime().exec(command);
      try (
          BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));) {
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line);
        }
        process.waitFor();
        log.debug("Output produced from command.... Output: {}", sb);
      } catch (Exception e) {
        log.error("Error executing command", e);
      }
    } catch (Exception e) {
      log.error("Error getting process from runtime...", e);
    }
    return sb.toString();
  }
}
