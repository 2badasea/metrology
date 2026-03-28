package com.bada.cali;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CaliApplication {
	public static void main(String[] args) {
		SpringApplication.run(CaliApplication.class, args);
	}
}
