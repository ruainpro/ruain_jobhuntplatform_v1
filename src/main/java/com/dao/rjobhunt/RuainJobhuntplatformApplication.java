package com.dao.rjobhunt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@EnableMongoAuditing
@SpringBootApplication
public class RuainJobhuntplatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(RuainJobhuntplatformApplication.class, args);
	}
	
}
