package com.example.BookWhiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableCaching
@EnableAsync        // makes EmailService.send() non-blocking
@EnableScheduling 
@SpringBootApplication
public class BookWhizApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookWhizApplication.class, args);
	}
}
