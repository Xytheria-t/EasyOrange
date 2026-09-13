package com.cartethyia.easyorange.framework.config.lock;

import com.cartethyia.easyorange.framework.config.properties.LockProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 分布式锁配置属性注册入口。
 * <p>
 * 锁的行为全在 {@link com.cartethyia.easyorange.framework.lock.DistributedRedissonLockAdapter}，
 * 这里只负责把 {@link LockProperties} 注册成 bean —— 注册位置不跟着适配器走，
 * 适配器被替换或移除时配置不会静默失效。
 */
@AutoConfiguration
@EnableConfigurationProperties(LockProperties.class)
public class LockConfig {}
