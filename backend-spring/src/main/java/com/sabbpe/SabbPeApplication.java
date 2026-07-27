package com.sabbpe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SabbPeApplication {

    public static void main(String[] args) {
        SpringApplication.run(SabbPeApplication.class, args);
    }
}
