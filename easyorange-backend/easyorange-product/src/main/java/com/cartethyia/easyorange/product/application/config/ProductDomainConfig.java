package com.cartethyia.easyorange.product.application.config;

import com.cartethyia.easyorange.product.domain.port.ProductCacheEvictionPort;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.service.ProductReportDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 商品模块领域服务装配 — domain 层零 Spring 依赖（ArchUnit 白名单约束），
 * 由本配置类在 application 边界显式接线。
 */
@Configuration
public class ProductDomainConfig {

    @Bean
    public ProductReportDomainService productReportDomainService(
            ProductReportRepository productReportRepository,
            ProductRepository productRepository,
            ProductCacheEvictionPort productCachePort) {
        return new ProductReportDomainService(productReportRepository, productRepository, productCachePort);
    }
}
