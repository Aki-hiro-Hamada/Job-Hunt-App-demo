package com.example.jobapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class JobApplicationApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobApplicationApplication.class, args);
	}
}
