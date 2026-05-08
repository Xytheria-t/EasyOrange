package com.cartethyia.easyorange.product.config;

import com.cartethyia.easyorange.product.domain.port.ProductCachePort;
import com.cartethyia.easyorange.product.domain.repository.ProductReportRepository;
import com.cartethyia.easyorange.product.domain.repository.ProductRepository;
import com.cartethyia.easyorange.product.domain.service.ProductReportDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductDomainConfig {

    @Bean
    public ProductReportDomainService productReportDomainService(
            ProductReportRepository productReportRepository,
            ProductRepository productRepository,
            ProductCachePort productCachePort) {
        return new ProductReportDomainService(productReportRepository, productRepository, productCachePort);
    }
}
