package com.co.jarvis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JarvisApplication {

	public static void main(String[] args) {
		SpringApplication.run(JarvisApplication.class, args);
	}

}
