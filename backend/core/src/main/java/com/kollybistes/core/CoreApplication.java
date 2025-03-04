package com.kollybistes.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
//@ComponentScan(basePackages = {"com.kollybistes.common"})
@EntityScan(basePackages = {"com.kollybistes.common.models", "com.kollybistes.core.models"})
@EnableJpaRepositories(basePackages = "com.kollybistes.core.repositories")
public class CoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreApplication.class, args);
    }
}
