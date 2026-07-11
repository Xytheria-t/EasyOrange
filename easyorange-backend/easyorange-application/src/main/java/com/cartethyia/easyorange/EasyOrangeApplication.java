package com.cartethyia.easyorange;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * @author cartethyia
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
@MapperScan(basePackages = "com.cartethyia.easyorange", annotationClass = Mapper.class)
public class EasyOrangeApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyOrangeApplication.class, args);
    }
}
