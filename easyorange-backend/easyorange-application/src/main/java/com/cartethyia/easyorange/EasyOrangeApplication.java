package com.cartethyia.easyorange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用入口 —— mapper 不开 {@code @MapperScan}：接口一律带 {@code @Mapper}，starter 按本包自动扫同一批；
 * 显式包扫描会让 IDEA 把包内所有接口当 mapper bean（机制与判据见 doc/agents/常用命令.md）。
 *
 * @author cartethyia
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class EasyOrangeApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyOrangeApplication.class, args);
    }
}
