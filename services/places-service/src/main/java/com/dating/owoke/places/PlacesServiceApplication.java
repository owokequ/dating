package com.dating.owoke.places;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PlacesServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlacesServiceApplication.class, args);
    }
}
