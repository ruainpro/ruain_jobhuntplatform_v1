package com.dao.rjobhunt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableMongoAuditing
@EnableScheduling
@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.dao.rjobhunt")
public class RuainJobhuntplatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(RuainJobhuntplatformApplication.class, args);
	}

}
