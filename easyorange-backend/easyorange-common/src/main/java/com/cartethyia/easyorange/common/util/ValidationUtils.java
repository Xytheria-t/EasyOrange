package com.cartethyia.easyorange.common.util;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public final class ValidationUtils {

    private ValidationUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * ValidatorFactory 实例，应用关闭时通过 shutdown hook 关闭
     */
    private static final ValidatorFactory VALIDATOR_FACTORY;

    /**
     * Validator 实例，线程安全
     */
    private static final Validator VALIDATOR;

    static {
        VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
        VALIDATOR = VALIDATOR_FACTORY.getValidator();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                VALIDATOR_FACTORY.close();
            } catch (Exception e) {
                log.warn("关闭 ValidatorFactory 失败: {}", e.getMessage());
            }
        }, "ValidationUtils-Shutdown-Hook"));
    }

    /**
     * 校验对象，返回所有约束违反
     *
     * @param obj 待校验对象
     * @return 约束违反集合，为空表示校验通过
     */
    public static <T> Set<ConstraintViolation<T>> validate(T obj) {
        if (obj == null) {
            return Set.of();
        }
        return VALIDATOR.validate(obj);
    }

    /**
     * 校验对象，返回第一个违反的消息（找不到返回 null）
     *
     * @param obj 待校验对象
     * @return 第一个错误消息，校验通过返回 null
     */
    public static <T> String validateFirst(T obj) {
        if (obj == null) {
            return "对象不能为 null";
        }
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(obj);
        if (violations.isEmpty()) {
            return null;
        }
        return violations.iterator().next().getMessage();
    }

    /**
     * 校验对象，校验失败时抛出异常
     *
     * @param obj         待校验对象
     * @param exceptionFn 将错误消息拼接为异常的函数
     * @throws RuntimeException 如果存在约束违反
     */
    public static <T> void validateOrThrow(T obj, java.util.function.Function<String, RuntimeException> exceptionFn) {
        String msg = validateFirst(obj);
        if (msg != null) {
            throw exceptionFn.apply(msg);
        }
    }

    /**
     * 校验对象，将违反信息按字段分组
     *
     * @param obj 待校验对象
     * @return 字段名 → 错误消息的映射
     */
    public static <T> Map<String, String> validateAsMap(T obj) {
        if (obj == null) {
            return Map.of();
        }
        return VALIDATOR.validate(obj).stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing + "; " + replacement
                ));
    }

    /**
     * 获取内部 Validator 实例（用于高级场景）
     */
    public static Validator getValidator() {
        return VALIDATOR;
    }
}
