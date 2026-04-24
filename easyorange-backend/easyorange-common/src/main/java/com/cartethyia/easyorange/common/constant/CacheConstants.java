package com.cartethyia.easyorange.common.constant;

import com.cartethyia.easyorange.common.util.BizRequire;

/**
 * 缓存常量
 * <p>
 * 按业务领域分组组织，所有过期时间统一以 <b>分钟</b> 为单位。
 * 常量名中不再带单位后缀（如 _HOURS），统一视为分钟。
 * 所有 key 统一以 {@code eo:} 前缀开头，避免多应用 Redis key 冲突。
 * </p>
 *
 * @author cartethyia
 */
public class CacheConstants {

    /**
     * 应用级 Redis key 前缀，避免多实例部署时 key 冲突
     */
    public static final String APP_PREFIX = "eo:";

    private CacheConstants() {
    }

    // ============ Login & Token ============

    /**
     * 登录与 Token 相关缓存
     */
    public static final class Login {
        private Login() {}

        /**
         * 登录 Token 缓存 Key前缀
         * <p>完整 Key 格式：eo:login_tokens:{token}
         * <p>Token 过期时间由 Spring Security JWT 配置管理
         */
        public static final String TOKEN_KEY = APP_PREFIX + "login_tokens:";

        /**
         * 登录失败次数缓存 Key前缀
         * <p>完整 Key 格式：eo:login:attempts:{username}
         * <p>过期时间：30 分钟（与登录锁定时间一致）
         */
        public static final String ATTEMPTS_KEY = APP_PREFIX + "login:attempts:";

        /**
         * 登录失败次数缓存过期时间（分钟）
         */
        public static final long ATTEMPTS_EXPIRE_TIME = 30L;

        public static String tokenKey(String token) {
            BizRequire.notNull(token, "token 不能为 null");
            return TOKEN_KEY + token;
        }

        public static String attemptsKey(String username) {
            BizRequire.notNull(username, "username 不能为 null");
            return ATTEMPTS_KEY + username;
        }
    }

    // ============ Product ============

    /**
     * 商品相关缓存
     */
    public static final class Product {
        private Product() {}

        /**
         * 商品信息缓存 Key前缀
         * <p>完整 Key 格式：eo:product:info:{productId}
         * <p>过期时间：60 分钟
         */
        public static final String INFO_KEY = APP_PREFIX + "product:info:";

        /**
         * 商品列表缓存 Key前缀
         * <p>完整 Key 格式：eo:product:list:{categoryId} 或 eo:product:list:{searchKey}
         */
        public static final String LIST_KEY = APP_PREFIX + "product:list:";

        /**
         * 商品信息缓存过期时间（分钟）
         */
        public static final long INFO_EXPIRE_TIME = 60L;

        /**
         * 商品列表缓存过期时间（分钟）
         */
        public static final long LIST_EXPIRE_TIME = 30L;

        public static String infoKey(Long productId) {
            BizRequire.notNull(productId, "productId 不能为 null");
            return INFO_KEY + productId;
        }
    }

    // ============ Category ============

    /**
     * 分类相关缓存
     */
    public static final class Category {
        private Category() {}

        /**
         * 分类列表缓存 Key
         */
        public static final String LIST_KEY = APP_PREFIX + "category:list";

        /**
         * 分类信息缓存过期时间（分钟）
         */
        public static final long INFO_EXPIRE_TIME = 120L;
    }

    // ============ Rate Limit & Repeat Submit ============

    /**
     * 限流与防重提交缓存
     */
    public static final class RateLimit {
        private RateLimit() {}

        /**
         * 防重复提交缓存 Key前缀
         * <p>完整 Key 格式：eo:repeat:submit:{token}:{method}:{params}
         * <p>过期时间由 @RepeatSubmit 注解的 interval 参数控制
         */
        public static final String REPEAT_SUBMIT_KEY = APP_PREFIX + "repeat:submit:";

        /**
         * 限流缓存 Key前缀
         * <p>完整 Key 格式：eo:rate:limit:{type}:{key}
         * <p>过期时间由 @RateLimiter 注解的 time 参数控制
         */
        public static final String KEY = APP_PREFIX + "rate:limit:";

        public static String repeatSubmitKey(String token, String methodKey, String paramsHash) {
            BizRequire.notNull(token, "repeatSubmitKey 参数 token 不能为 null");
            BizRequire.notNull(methodKey, "repeatSubmitKey 参数 methodKey 不能为 null");
            BizRequire.notNull(paramsHash, "repeatSubmitKey 参数 paramsHash 不能为 null");
            return REPEAT_SUBMIT_KEY + token + ":" + methodKey + ":" + paramsHash;
        }

        public static String key(String type, String key) {
            BizRequire.notNull(type, "rateLimitKey 参数 type 不能为 null");
            BizRequire.notNull(key, "rateLimitKey 参数 key 不能为 null");
            return KEY + type + ":" + key;
        }
    }

    // ============ Order ============

    /**
     * 订单相关缓存
     */
    public static final class Order {
        private Order() {}

        /**
         * 订单详情缓存 Key前缀
         * <p>完整 Key 格式：eo:order:detail:{orderId}
         * <p>过期时间：30 分钟
         */
        public static final String DETAIL_KEY = APP_PREFIX + "order:detail:";

        /**
         * 订单详情缓存过期时间（分钟）
         */
        public static final long DETAIL_EXPIRE_TIME = 30L;

        public static String detailKey(Long orderId) {
            BizRequire.notNull(orderId, "orderId 不能为 null");
            return DETAIL_KEY + orderId;
        }
    }

}
