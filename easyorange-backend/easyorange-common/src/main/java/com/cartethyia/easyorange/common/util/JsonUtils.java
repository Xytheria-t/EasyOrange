package com.cartethyia.easyorange.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JSON 工具类
 * <p>
 * 基于 Jackson ObjectMapper 封装，提供常用的序列化/反序列化操作。
 * 内部使用预配置的 ObjectMapper 实例（线程安全），支持 Java 8 时间类型。
 * </p>
 *
 * <h3>ObjectMapper 配置优先级：</h3>
 * <ol>
 *   <li>通过 {@link #setMapper(ObjectMapper)} 注入的外部实例（通常为 Spring 管理的 Bean）</li>
 *   <li>内置默认实例（使用基础配置，不读取 spring.jackson.* 配置）</li>
 * </ol>
 *
 * <pre>{@code
 * // 用法示例
 * String json = JsonUtils.toJson(user);
 * User user = JsonUtils.fromJson(json, User.class);
 * List<User> users = JsonUtils.fromJsonList(json, User.class);
 *
 * // Spring 环境中注入自定义 ObjectMapper（可选）
 * JsonUtils.setMapper(customObjectMapper);
 * }</pre>
 *
 * @author cartethyia
 */
public final class JsonUtils {

    private JsonUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 内置默认 ObjectMapper（当未通过 {@link #setMapper} 注入时使用）
     */
    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    /**
     * 当前使用的 ObjectMapper，可通过 {@link #setMapper(ObjectMapper)} 替换为 Spring 管理的实例
     */
    private static volatile ObjectMapper mapper = DEFAULT_MAPPER;

    // ==================== 配置 ====================

    /**
     * 设置自定义 ObjectMapper（线程安全）
     * <p>
     * 推荐在 Spring Boot 启动时通过 {@code @PostConstruct} 注入，
     * 这样可自动读取 spring.jackson.* 配置（日期格式、命名策略等）。
     * </p>
     *
     * @param customMapper 自定义 ObjectMapper 实例
     */
    public static void setMapper(ObjectMapper customMapper) {
        mapper = Objects.requireNonNull(customMapper, "ObjectMapper 不能为 null");
    }

    /**
     * 获取当前使用的 ObjectMapper 实例
     * <p>
     * 注意：返回的实例是共享的，不建议修改其配置。
     * 如需自定义配置，请创建新的 ObjectMapper 实例。
     * </p>
     */
    public static ObjectMapper getMapper() {
        return mapper;
    }

    // ==================== 序列化 ====================

    /**
     * 对象转 JSON 字符串
     *
     * @throws JsonException 序列化失败时抛出，包含失败对象类型信息
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonException("JSON 序列化失败: " + obj.getClass().getName(), e);
        }
    }

    /**
     * 对象转格式化的 JSON 字符串（便于调试）
     *
     * @throws JsonException 序列化失败时抛出
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonException("JSON 格式化失败: " + obj.getClass().getName(), e);
        }
    }

    // ==================== 反序列化 ====================

    /**
     * JSON 字符串转对象
     *
     * @throws JsonException 反序列化失败时抛出
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new JsonException("JSON 反序列化失败: " + clazz.getName(), e);
        }
    }

    /**
     * JSON 字符串转对象（支持泛型 TypeReference）
     *
     * @throws JsonException 反序列化失败时抛出
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readValue(json, typeReference);
        } catch (IOException e) {
            throw new JsonException("JSON 反序列化失败", e);
        }
    }

    /**
     * JSON 字符串转 List
     *
     * @throws JsonException 反序列化失败时抛出
     */
    public static <T> List<T> fromJsonList(String json, Class<T> elementClass) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, elementClass));
        } catch (IOException e) {
            throw new JsonException("JSON 反序列化 List 失败: " + elementClass.getName(), e);
        }
    }

    /**
     * JSON 字符串转 Map
     *
     * @throws JsonException 反序列化失败时抛出
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> fromJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(json, Map.class);
        } catch (IOException e) {
            throw new JsonException("JSON 反序列化 Map 失败", e);
        }
    }

    /**
     * 从 InputStream 读取 JSON 并反序列化为对象
     *
     * @throws JsonException 反序列化失败时抛出
     */
    public static <T> T fromJson(InputStream is, Class<T> clazz) {
        if (is == null) {
            return null;
        }
        try {
            return mapper.readValue(is, clazz);
        } catch (IOException e) {
            throw new JsonException("JSON 反序列化失败: " + clazz.getName(), e);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 深拷贝对象（通过 JSON 序列化/反序列化实现）
     */
    public static <T> T deepCopy(T obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        return fromJson(toJson(obj), clazz);
    }

    /**
     * 判断字符串是否为合法 JSON
     */
    public static boolean isJson(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }
        try {
            mapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * JSON 字符串转 JsonNode
     *
     * @throws JsonException 解析失败时抛出
     */
    public static com.fasterxml.jackson.databind.JsonNode toJsonNode(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return mapper.readTree(json);
        } catch (IOException e) {
            throw new JsonException("JSON 解析为 JsonNode 失败", e);
        }
    }

    /**
     * JsonNode 转对象
     *
     * @throws JsonException 反序列化失败时抛出
     */
    public static <T> T fromJsonNode(com.fasterxml.jackson.databind.JsonNode node, Class<T> clazz) {
        if (node == null) {
            return null;
        }
        try {
            return mapper.treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new JsonException("JsonNode 反序列化失败: " + clazz.getName(), e);
        }
    }

    // ==================== 自定义异常 ====================

    /**
     * JSON 序列化/反序列化异常
     * <p>
     * 替代原始的 RuntimeException 包装，提供更明确的异常类型和更好的错误信息。
     * </p>
     */
    public static class JsonException extends RuntimeException {

        @java.io.Serial
        private static final long serialVersionUID = 1L;

        public JsonException(String message, Throwable cause) {
            super(message, cause);
        }

        public JsonException(String message) {
            super(message);
        }
    }
}
