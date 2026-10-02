package com.cartethyia.easyorange.framework.util;

import com.cartethyia.easyorange.common.enums.ResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Jackson 静默 / 硬失败读写助手 —— 收敛「try + catch JacksonException + 兜底」的重复样板
 * （audit 时全仓 6 处逐字雷同）。两种语义按需选用，不要混用：
 * <ul>
 *   <li>{@code *Quietly} — 失败返回 null，调用方按自身语义告警 / 跳过 / 取默认值
 *       （审计日志字段、维度列表这类「丢了不伤主数据」的副产物）；</li>
 *   <li>{@code *Required} — 失败抛 {@link BusinessException}（INTERNAL_SERVER_ERROR），
 *       订单快照落库这类「写不出就该让事务回滚」的主数据路径。</li>
 * </ul>
 */
public final class Jsons {

    private Jsons() {}

    /** 序列化，失败返回 null（绝不抛出）。 */
    public static String writeQuietly(ObjectMapper mapper, Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JacksonException e) {
            return null;
        }
    }

    /** 泛型容器形态的反序列化，失败返回 null（绝不抛出）。 */
    public static <T> T readQuietly(ObjectMapper mapper, String json, TypeReference<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JacksonException e) {
            return null;
        }
    }

    /** 序列化，失败抛 BusinessException —— 主数据写路径用（静默 null 会把坏数据写进 DB）。 */
    public static String writeRequired(ObjectMapper mapper, Object value, String message) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw BusinessException.of(ResultCode.INTERNAL_SERVER_ERROR, message, e);
        }
    }

    /** 反序列化，失败抛 BusinessException —— 主数据读路径用。 */
    public static <T> T readRequired(ObjectMapper mapper, String json, Class<T> type, String message) {
        try {
            return mapper.readValue(json, type);
        } catch (JacksonException e) {
            throw BusinessException.of(ResultCode.INTERNAL_SERVER_ERROR, message, e);
        }
    }
}
