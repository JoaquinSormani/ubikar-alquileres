package com.ubikar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UbikarApplication {

    public static void main(String[] args) {
        System.setProperty("server.port", "8081");
        SpringApplication.run(UbikarApplication.class, args);
    }
}
