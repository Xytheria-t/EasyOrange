package com.cartethyia.easyorange.framework.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * ES 检索开关。
 * <p>
 * 关掉不是「功能不可用」而是「功能静默变差」——召回降级为 MySQL LIKE，只命中字面重叠；生产由
 * {@code ProdSearchGuard} 拒绝以该形态启动，健康检查另报 UNKNOWN。
 *
 * @param enabled 是否启用 ES 检索（dev / prod 显式 true；缺省 false 供 it 与裸跑基文件用）
 */
@ConfigurationProperties(prefix = "easyorange.search.elasticsearch")
public record ElasticsearchProperties(@DefaultValue("false") boolean enabled) {}
