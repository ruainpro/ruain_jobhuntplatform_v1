package com.dao.rjobhunt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@EnableMongoAuditing
@SpringBootApplication
public class RuainJobhuntplatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(RuainJobhuntplatformApplication.class, args);
	}
	
}
