package com.sprint.tms_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TmsBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TmsBackendApplication.class, args);
	}
}