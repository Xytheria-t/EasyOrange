package com.cartethyia.easyorange;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * @author cartethyia
 */
@SpringBootApplication
@MapperScan("com.cartethyia.easyorange.**.mapper")
@EnableScheduling
public class EasyOrangeApplication {
    static void main(String[] args) {
        SpringApplication.run(EasyOrangeApplication.class, args);
    }
}
