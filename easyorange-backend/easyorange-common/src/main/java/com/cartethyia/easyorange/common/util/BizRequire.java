package com.cartethyia.easyorange.common.util;

import com.cartethyia.easyorange.common.enums.IResultCode;
import com.cartethyia.easyorange.common.exception.BusinessException;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 业务校验工具类
 *
 * <p>核心规则：所有方法在"条件满足时抛出 BusinessException"</p>
 *
 * <h3>方法命名规律：</h3>
 * <ul>
 *   <li>{@code notXxx} - 验证"不能为X"，是X则抛异常</li>
 *   <li>{@code requireXxx} - 验证"必须满足X"，不满足则抛异常</li>
 * </ul>
 *
 * <h3>选择指南：</h3>
 * <pre>
 * 对象为null时抛异常        → notNull(obj, msg)
 * 对象不为null时抛异常      → isNull(obj, msg)
 * 字符串为空时抛异常        → notBlank(str, msg)
 * 集合为空时抛异常          → notEmpty(collection, msg)
 * 数字≤0时抛异常            → positive(num, msg)
 * 数字<0时抛异常             → nonNegative(num, msg)
 * 布尔必须为true时抛异常    → requireTrue(cond, msg)
 * 布尔必须为false时抛异常   → requireFalse(cond, msg)
 * 直接抛异常                 → fail(msg)
 * </pre>
 *
 * <h3>示例：</h3>
 * <pre>
 * // 验证用户必须存在
 * BizRequire.notNull(user, "用户不存在");
 *
 * // 验证商品已下架（状态不是ONLINE时抛异常）
 * BizRequire.requireTrue(ProductStatus.ONLINE.getCode().equals(status), "商品已下架");
 *
 * // 验证不能买自己商品（是卖家时抛异常）
 * BizRequire.requireFalse(product.getSellerId().equals(userId), "不能购买自己的商品");
 * </pre>
 */
public final class BizRequire {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private BizRequire() {
        throw new IllegalStateException("Utility class");
    }

    public static void notNull(Object obj, String message) {
        if (obj == null) {
            throw BusinessException.of(message);
        }
    }

    public static void notNull(Object obj, IResultCode resultCode) {
        if (obj == null) {
            throw BusinessException.of(resultCode);
        }
    }

    public static void isNull(Object obj, String message) {
        if (obj != null) {
            throw BusinessException.of(message);
        }
    }

    /**
     * 验证条件必须为 true，不为 true 时抛异常
     *
     * <p>示例：商品必须在线</p>
     * <pre>BizRequire.requireTrue(product.getStatus() == 1, "商品已下架");</pre>
     */
    public static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw BusinessException.of(message);
        }
    }

    public static void requireTrue(boolean condition, IResultCode resultCode) {
        if (!condition) {
            throw BusinessException.of(resultCode);
        }
    }

    /**
     * 验证条件必须为 false，为 false 时抛异常
     *
     * <p>示例：用户不能是卖家</p>
     * <pre>BizRequire.requireFalse(product.getSellerId().equals(userId), "不能购买自己的商品");</pre>
     */
    public static void requireFalse(boolean condition, String message) {
        if (condition) {
            throw BusinessException.of(message);
        }
    }

    /**
     * 验证字符串不能为空（null、""、" " 都视为空）
     */
    public static void notBlank(String str, String message) {
        if (str == null || str.isBlank()) {
            throw BusinessException.of(message);
        }
    }

    public static void notBlank(String str, IResultCode resultCode) {
        if (str == null || str.isBlank()) {
            throw BusinessException.of(resultCode);
        }
    }

    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) {
            throw BusinessException.of(message);
        }
    }

    public static void notEmpty(Map<?, ?> map, String message) {
        if (map == null || map.isEmpty()) {
            throw BusinessException.of(message);
        }
    }

    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) {
            throw BusinessException.of(message);
        }
    }

    /**
     * 验证数字必须大于 0
     */
    public static void positive(Number number, String message) {
        if (number == null || number.longValue() <= 0) {
            throw BusinessException.of(message);
        }
    }

    /**
     * 验证数字必须大于等于 0
     */
    public static void nonNegative(Number number, String message) {
        if (number == null || number.longValue() < 0) {
            throw BusinessException.of(message);
        }
    }

    public static void between(long value, long min, long max, String message) {
        if (value < min || value > max) {
            throw BusinessException.of(message);
        }
    }

    public static void noNullElements(Collection<?> collection, String message) {
        if (collection != null) {
            for (Object obj : collection) {
                if (obj == null) {
                    throw BusinessException.of(message);
                }
            }
        }
    }

    /**
     * 验证邮箱格式
     */
    public static void validEmail(String email, String message) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw BusinessException.of(message);
        }
    }

    /**
     * 验证手机号格式（中国大陆手机号）
     */
    public static void validPhone(String phone, String message) {
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw BusinessException.of(message);
        }
    }

    /**
     * 直接抛出异常
     */
    public static void fail(String message) {
        throw BusinessException.of(message);
    }

    /**
     * 验证两个值必须相等
     */
    public static void eq(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw BusinessException.of(message);
        }
    }

    public static void eq(Object expected, Object actual, IResultCode resultCode) {
        if (!Objects.equals(expected, actual)) {
            throw BusinessException.of(resultCode);
        }
    }

    /**
     * 验证两个值必须不相等
     */
    public static void ne(Object expected, Object actual, String message) {
        if (Objects.equals(expected, actual)) {
            throw BusinessException.of(message);
        }
    }
}