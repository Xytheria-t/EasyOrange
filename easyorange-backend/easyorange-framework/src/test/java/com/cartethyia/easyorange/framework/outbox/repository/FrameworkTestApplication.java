package com.cartethyia.easyorange.framework.outbox.repository;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = "com.cartethyia.easyorange.framework.outbox",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.cartethyia\\.easyorange\\.framework\\.outbox\\.publisher\\..*"
        )
)
@MapperScan("com.cartethyia.easyorange.framework.outbox.mapper")
public class FrameworkTestApplication {
}
