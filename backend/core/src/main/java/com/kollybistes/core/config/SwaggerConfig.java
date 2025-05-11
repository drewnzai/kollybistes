package com.kollybistes.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI myOpenAPI(){

        Contact contact = new Contact();
        contact.setEmail("andrewnzaikombe@gmail.com");
        contact.setName("Andrew Kombe");

        License gplv3 = new License().name("GNU General Public License v3.0")
                .url("https://choosealicense.com/licenses/gpl-3.0/");

        Info info = new Info()
                .title("Ethereum-Bitcoin Exchange Backend API")
                .version("1.0.0")
                .contact(contact)
                .description("This API exposes backend endpoints.")
                .license(gplv3);

        return new OpenAPI().info(info);
    }
}
