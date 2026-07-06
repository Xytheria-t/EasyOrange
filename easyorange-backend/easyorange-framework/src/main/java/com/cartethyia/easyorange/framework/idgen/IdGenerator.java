package com.cartethyia.easyorange.framework.idgen;

/**
 * 分布式 ID 生成器接口（Port 抽象）
 * <p>
 * 主实现：UUID v7（RFC 9562），零配置零依赖
 * 领域层和应用层通过此接口获取 ID，与具体算法解耦。
 */
@FunctionalInterface
public interface IdGenerator {

    /**
     * 生成唯一 ID（字符串格式）
     */
    String generateId();
}
