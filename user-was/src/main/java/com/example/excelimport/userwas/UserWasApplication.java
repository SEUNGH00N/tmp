package com.example.excelimport.userwas;

import com.example.excelimport.userwas.storage.UserStorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(UserStorageProperties.class)
public class UserWasApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserWasApplication.class, args);
    }
}
