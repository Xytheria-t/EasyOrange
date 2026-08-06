package com.cartethyia.easyorange.config.health;

import com.cartethyia.easyorange.ai.config.AiProperties;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * AI 供应商健康指示器 — 报告 AI 链路是否处于「可用」状态。
 * <p>
 * AI 是可选降级功能（key 缺失时以代理模型 placeholder 运行，应用照常启动），因此这里做
 * <b>配置状态</b>探测而非实时连通性探测：实时探测会在每次健康检查时发起真实 LLM 调用，
 * 拖慢探针且产生计费成本。运维关注的是「AI 是否真的可用（而非降级）」，配置状态即可回答。
 * <ul>
 *   <li>至少一个供应商配置 → {@code UP}</li>
 *   <li>全部未配置 → {@code UNKNOWN}（可选功能未启用，不属于故障，不拉低整体健康）</li>
 * </ul>
 * 若需实时连通性探测，可在 readiness 探针之外另建专项巡检任务。
 */
@Component
public class AiHealthIndicator implements HealthIndicator {

    private final AiProperties properties;

    public AiHealthIndicator(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        var builder = Health.up();
        int configured = 0;

        configured += describeProvider(builder, "chat", properties.getDeepseek().getApiKey());
        configured += describeProvider(builder, "vision", properties.getQwenVl().getApiKey());
        configured +=
                describeProvider(builder, "embedding", properties.getEmbedding().getApiKey());

        builder.withDetail("configuredProviders", configured);
        if (configured == 0) {
            return Health.unknown()
                    .withDetail("reason", "no AI provider configured — AI is an optional, degradable feature")
                    .build();
        }
        return builder.build();
    }

    private int describeProvider(Health.Builder builder, String name, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            builder.withDetail(name, "unconfigured");
            return 0;
        }
        builder.withDetail(name, "configured");
        return 1;
    }
}
