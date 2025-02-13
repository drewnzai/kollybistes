package com.kollybistes.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.kollybistes.api", "com.kollybistes.core", "com.kollybistes.common"})
@EntityScan(basePackages = "com.kollybistes.common.models")
@EnableJpaRepositories(basePackages = "com.kollybistes.core.repositories")
public class APIApplication {
    public static void main(String[] args) {
        SpringApplication.run(APIApplication.class, args);
    }
}
