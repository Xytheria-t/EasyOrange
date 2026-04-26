package com.cartethyia.easyorange.framework.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 操作日志配置属性
 * <p>
 * 用于控制操作日志的记录行为，包括是否启用、日志级别、
 * 是否保存请求/响应数据等。
 * </p>
 * 配置示例：
 * <pre>{@code
 * operlog:
 *   enabled: true
 *   log-level: DETAILED
 *   save-request-data: false
 *   save-response-data: false
 * }</pre>
 */
@Setter
@Getter
@Component
@ToString
@ConfigurationProperties(prefix = "operlog")
public class OperLogProperties {

    /**
     * 是否启用操作日志记录
     * <p>
     * 默认为 true，设置为 false 时完全禁用日志记录功能
     * </p>
     */
    private boolean enabled = true;

    /**
     * 日志记录级别
     * <p>
     * - DISABLED: 不记录任何日志
     * - BASIC: 仅记录基本信息（操作时间、操作人、IP、操作类型）
     * - DETAILED: 记录完整信息（包含请求参数和响应数据）
     * </p>
     * 默认为 BASIC
     */
    private LogLevel logLevel = LogLevel.BASIC;

    /**
     * 是否保存请求数据
     * <p>
     * 默认为 true，保存请求参数到 oper_param 字段
     * </p>
     */
    private boolean saveRequestData = true;

    /**
     * 是否保存响应数据
     * <p>
     * 默认为 false，设置为 true 时会保存 JSON 响应到 json_result 字段
     * 注意：开启此选项会增加数据库存储压力，生产环境建议保持关闭
     * </p>
     */
    private boolean saveResponseData = false;

    /**
     * 日志记录级别枚举
     */
    public enum LogLevel {
        /** 禁用日志 */
        DISABLED,
        /** 仅记录基本信息 */
        BASIC,
        /** 记录完整信息 */
        DETAILED
    }
}
