package com.redali.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point into the application.
 *
 * <p>Standard Java entry point (where everything starts). We will just hand control over to spring-boot.</p>
 */
@SpringBootApplication
@EnableScheduling
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
