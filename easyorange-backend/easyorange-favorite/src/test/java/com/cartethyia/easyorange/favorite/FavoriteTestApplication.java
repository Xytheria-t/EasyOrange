package com.cartethyia.easyorange.favorite;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.cartethyia.easyorange.favorite")
@MapperScan("com.cartethyia.easyorange.favorite.infrastructure.persistence")
public class FavoriteTestApplication {
}
