package com.cartethyia.easyorange.framework.util;

/**
 * 审计日志工具类
 * <p>
 * 提供字符串截断等工具方法。
 * 模块名推导和操作标题推导参见 {@link com.cartethyia.easyorange.framework.audit.aspect.AuditLogAspect}，
 * 映射数据源自 {@link com.cartethyia.easyorange.framework.config.properties.AuditLogProperties}。
 * </p>
 *
 * @author cartethyia
 */
public final class AuditLogUtil {

    private AuditLogUtil() {
        throw new IllegalStateException("Utility class");
    }

    private static final String TRUNCATE_SUFFIX = "...(已截断)";

    /**
     * 截断字符串到指定长度
     *
     * @param str       原字符串
     * @param maxLength 最大长度
     * @return 截断后的字符串
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength must be non-negative, actual: " + maxLength);
        }
        if (str.length() <= maxLength) {
            return str;
        }

        int truncateLength = maxLength - TRUNCATE_SUFFIX.length();
        if (truncateLength <= 0) {
            return str.substring(0, maxLength);
        }
        return str.substring(0, truncateLength) + TRUNCATE_SUFFIX;
    }
}
