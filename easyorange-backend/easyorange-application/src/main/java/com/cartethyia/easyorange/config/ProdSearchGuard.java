package com.cartethyia.easyorange.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 生产环境检索依赖守卫 — ES 是 RAG 的混合召回后端，生产不接受静默降级。
 * <p>
 * 与 {@link ProdSecrets} 同一契约风格：生产必需依赖缺失或被关闭时启动即中止，而不是运行时降级。
 * {@code application-prod.yaml} 已显式启用 ES；本守卫拦的是「被环境变量覆盖回 false」这类隐式关闭
 * —— Spring 的 relaxed binding 下环境变量优先级高于 yaml，仅靠配置文件默认值挡不住。
 * <p>
 * 关闭 ES 的后果不是「功能不可用」而是「功能静默变差」：检索走 MySQL LIKE，同一金标准集实测
 * hit@5 10%，对比 ES 路径 100%。这类降级必须被显式选择，不能由覆盖或默认值无意造成。
 * <p>
 * ES 启用但不可达时另有兜底：{@code ElasticsearchIndexManager} 的 {@code @PostConstruct}
 * 建索引失败会抛异常，同样中止启动。
 */
@Configuration
@Profile("prod")
public class ProdSearchGuard {

    public ProdSearchGuard(@Value("${easyorange.search.elasticsearch.enabled:false}") boolean esEnabled) {
        if (!esEnabled) {
            throw new IllegalStateException("生产环境必须启用 ES（easyorange.search.elasticsearch.enabled=true）："
                    + "RAG 检索在无 ES 时降级为 MySQL LIKE，同一金标准集实测 hit@5 仅 10%。"
                    + "如确需接受该降级，请显式移除本守卫并把决策记入 ADR。");
        }
    }
}
