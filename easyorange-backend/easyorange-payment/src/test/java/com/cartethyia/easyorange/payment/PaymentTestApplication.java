package com.cartethyia.easyorange.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.cartethyia.easyorange.payment")
@MapperScan("com.cartethyia.easyorange.payment.infrastructure.persistence")
public class PaymentTestApplication {
}
