package com.mycompany.app;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
@Slf4j
@Profile("cyberark")
@Component
public class CyberarkPasswordFetcher implements PasswordFetcher{

    @Autowired
    private ShellCommandExecutor shellCommandExecutor;

    //TODO this will use our cyberark dependencies.
    public String fetch() throws Exception {
        log.trace("fetch()");
        try { 
            return shellCommandExecutor
                    .execute("grep '^DATASOURCE_PASSWORD=' /workspaces/vscode-remote-try-java/.env | cut -d '=' -f2-");
        }catch(Exception e){
            log.error("Error fetching password", e);
            throw e;
        }
    }
}
