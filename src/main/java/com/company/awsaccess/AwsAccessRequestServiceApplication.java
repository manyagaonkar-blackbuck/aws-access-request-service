package com.company.awsaccess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AwsAccessRequestServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AwsAccessRequestServiceApplication.class, args);
    }
}
