package com.cartethyia.easyorange.product;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 控制器切片测试（@WebMvcTest）的引导配置。用 {@code @SpringBootApplication} 而非裸
 * {@code @ComponentScan} —— 只有前者才挂上 Boot 的 TypeExcludeFilter，切片才会按
 * {@code @WebMvcTest(XxxController.class)} 排除其它控制器；裸扫描拿不到排除，
 * 同包的两个控制器测试会互相装载对方控制器、依赖全缺（2026-09-23 实测踩坑）。
 * 切片内的其余 bean（application/持久化/适配器）由各测试用 {@code @MockitoBean} 提供。
 */
@SpringBootApplication
public class ProductTestApplication {}
