package com.subtracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SubtrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SubtrackerApplication.class, args);
	}


}
