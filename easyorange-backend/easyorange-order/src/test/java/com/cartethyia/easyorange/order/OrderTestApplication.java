package com.cartethyia.easyorange.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.cartethyia.easyorange.order")
@MapperScan("com.cartethyia.easyorange.order.adapter.outbound.persistence")
public class OrderTestApplication {
}
