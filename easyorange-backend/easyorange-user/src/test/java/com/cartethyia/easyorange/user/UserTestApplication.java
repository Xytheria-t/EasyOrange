package com.cartethyia.easyorange.user;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 控制器切片测试（@WebMvcTest）的引导配置。用 @SpringBootApplication 而非裸 @ComponentScan，
 * 以获得 Boot 的 TypeExcludeFilter：切片只装 web 组件（控制器/advice），
 * 其余 bean（application/domain/持久化）由各测试用 @MockitoBean 提供。
 */
@SpringBootApplication
public class UserTestApplication {}
