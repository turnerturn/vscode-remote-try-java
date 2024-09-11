package com.mycompany.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import io.github.cdimascio.dotenv.Dotenv;

@Slf4j
@Profile("!cyberark")
@Component
public class LocalPasswordFetcher implements PasswordFetcher {

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
