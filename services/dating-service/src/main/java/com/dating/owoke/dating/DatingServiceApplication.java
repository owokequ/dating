package com.dating.owoke.dating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DatingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatingServiceApplication.class, args);
    }
}
