package com.cartethyia.easyorange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * @author cartethyia
 */
@SpringBootApplication
@EnableScheduling
public class EasyOrangeApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyOrangeApplication.class, args);
    }
}
