package com.cartethyia.easyorange.framework.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.framework.config.properties.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Refresh 端点 CSRF 纵深防御过滤器 — 单元测试。
 * <p>
 * 验证：对 csrf-protected 路径的 POST 强制要求 X-Client-Type 头（跨站请求无法伪造），缺失返回 403；
 * 非 POST / 非保护路径不拦截。
 */
@DisplayName("Refresh CSRF 过滤器")
class RefreshCsrfFilterTest {

    private RefreshCsrfFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RefreshCsrfFilter(new SecurityProperties(), new ObjectMapper());
    }

    @Test
    @DisplayName("POST 保护路径携带 X-Client-Type 放行")
    void postWithHeader_passes() throws Exception {
        var req = new MockHttpServletRequest("POST", "/api/auth/refresh");
        req.addHeader("X-Client-Type", "web");
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("POST 保护路径缺失自定义头返回 403")
    void postWithoutHeader_returns403() throws Exception {
        var req = new MockHttpServletRequest("POST", "/api/auth/refresh");
        var res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        assertThat(res.getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("GET 保护路径不拦截")
    void getIsNotFiltered() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/auth/refresh");
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("POST 非保护路径不拦截")
    void postToUnprotected_passes() throws Exception {
        var req = new MockHttpServletRequest("POST", "/api/products");
        var res = new MockHttpServletResponse();
        var chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}
