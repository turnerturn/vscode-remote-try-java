package com.mycompany.app;

import java.util.UUID;

import org.springframework.cloud.context.restart.RestartEndpoint;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

//import org.springframework.cloud.context.restart.RestartEndpoint;
import lombok.extern.slf4j.Slf4j;

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

