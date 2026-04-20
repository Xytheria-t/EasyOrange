package com.cartethyia.easyorange.common.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Spring 容器静态访问工具类
 * <p>
 * 在非 Spring 管理的类中获取 Bean 或配置信息。
 * 适用于工具类、静态方法、AOP 切面等场景。
 * </p>
 *
 * <pre>{@code
 * // 用法示例
 * UserService userService = SpringUtils.getBean(UserService.class);
 * String port = SpringUtils.getProperty("server.port");
 * }</pre>
 *
 * @author cartethyia
 */
@Component
public class SpringUtils implements ApplicationContextAware {

    private static volatile ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtils.context = applicationContext;
    }

    /**
     * 获取 ApplicationContext
     *
     * @throws IllegalStateException 如果 Spring 容器尚未初始化
     */
    public static ApplicationContext getContext() {
        requireContext();
        return context;
    }

    /**
     * 根据类型获取 Bean
     *
     * @throws IllegalStateException 如果 Spring 容器尚未初始化
     */
    public static <T> T getBean(Class<T> clazz) {
        return getContext().getBean(clazz);
    }

    /**
     * 根据名称获取 Bean
     *
     * @throws IllegalStateException 如果 Spring 容器尚未初始化
     */
    public static Object getBean(String name) {
        return getContext().getBean(name);
    }

    /**
     * 根据名称和类型获取 Bean
     *
     * @throws IllegalStateException 如果 Spring 容器尚未初始化
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return getContext().getBean(name, clazz);
    }

    /**
     * 获取配置属性值
     *
     * @throws IllegalStateException 如果 Spring 容器尚未初始化
     */
    public static String getProperty(String key) {
        return getContext().getBean(Environment.class).getProperty(key);
    }

    /**
     * 获取配置属性值（带默认值）
     *
     * @throws IllegalStateException 如果 Spring 容器尚未初始化
     */
    public static String getProperty(String key, String defaultValue) {
        return getContext().getBean(Environment.class).getProperty(key, defaultValue);
    }

    /**
     * 获取当前激活的 profile
     *
     * @throws IllegalStateException 如果 Spring 容器尚未初始化
     */
    public static String[] getActiveProfiles() {
        return getContext().getBean(Environment.class).getActiveProfiles();
    }

    /**
     * 检查 Spring 容器是否已初始化（不抛出异常）
     *
     * @return true 如果容器已就绪
     */
    public static boolean isReady() {
        return context != null;
    }

    /**
     * 根据类型获取 Bean（不存在时返回 Optional.empty）
     */
    public static <T> java.util.Optional<T> getOptionalBean(Class<T> clazz) {
        if (!isReady()) {
            return java.util.Optional.empty();
        }
        String[] beanNames = context.getBeanNamesForType(clazz);
        if (beanNames.length == 0) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(context.getBean(clazz));
    }

    /**
     * 内部方法：确保容器已初始化，否则抛出明确的异常
     */
    private static void requireContext() {
        Objects.requireNonNull(context,
                "ApplicationContext 未初始化。请确保 Spring 容器已启动，且 SpringUtils Bean 已被扫描注册。");
    }
}
