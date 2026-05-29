package com.cartethyia.easyorange.order;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Minimal test application for controller slice tests (@WebMvcTest).
 * Only scans the web adapter package; all other beans (application, domain,
 * persistence, and adapter/outbound) are provided via @MockitoBean mocks in the
 * controller test classes. The @WebMvcTest annotation provides its own
 * targeted auto-configuration, so @EnableAutoConfiguration is intentionally
 * omitted to avoid pulling in MyBatis/DataSource beans.
 * Integration tests requiring full context should use OrderIntegrationTestConfig instead.
 */
@SpringBootConfiguration
@ComponentScan(basePackages = "com.cartethyia.easyorange.order.adapter.inbound.web")
public class OrderTestApplication {
}
