package com.example.ses;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SesApplication.class, args);
    }
}
