package com.runningolle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RunningOlleApplication {
    public static void main(String[] args) {
        SpringApplication.run(RunningOlleApplication.class, args);
    }
}
