package com.cartethyia.easyorange.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Integration test configuration with full component scanning and MyBatis mapper scanning.
 * Used by integration tests that need the full application context.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.cartethyia.easyorange.order")
@MapperScan("com.cartethyia.easyorange.order.adapter.outbound.persistence")
public class OrderIntegrationTestConfig {
}
