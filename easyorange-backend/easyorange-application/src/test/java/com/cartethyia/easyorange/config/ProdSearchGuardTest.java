package com.cartethyia.easyorange.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * 生产检索依赖守卫测试 — 生产不接受 RAG 静默降级：ES 被关闭（含属性缺失、被覆盖回 false）
 * 时启动必须失败；启用时正常；非 prod profile 不得触发（守卫只认 prod，检索开关由各 profile 自己决定）。
 */
class ProdSearchGuardTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(ProdSearchGuard.class);

    @Test
    void failsFastWhenEsDisabledInProd() {
        runner.withPropertyValues("spring.profiles.active=prod", "easyorange.search.elasticsearch.enabled=false")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsFastWhenEsPropertyAbsentInProd() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void startsWhenEsEnabledInProd() {
        runner.withPropertyValues("spring.profiles.active=prod", "easyorange.search.elasticsearch.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ProdSearchGuard.class);
                });
    }

    @Test
    void notActivatedOutsideProdProfile() {
        runner.withPropertyValues("spring.profiles.active=dev", "easyorange.search.elasticsearch.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ProdSearchGuard.class);
                });
    }
}
