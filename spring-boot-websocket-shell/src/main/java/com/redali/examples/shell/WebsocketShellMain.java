package com.redali.examples.shell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.shell.command.annotation.EnableCommand;

@SpringBootApplication
@EnableCommand({ SystemCommands.class, TimeCommands.class })
public class WebsocketShellMain {

    public static void main(String[] args) {
        SpringApplication.run(WebsocketShellMain.class, args);
    }

}
