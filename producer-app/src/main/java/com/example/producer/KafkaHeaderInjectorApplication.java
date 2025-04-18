package com.example.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KafkaHeaderInjectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(KafkaHeaderInjectorApplication.class, args);
    }
} 