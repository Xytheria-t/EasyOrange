package com.cartethyia.easyorange.framework.idgen;

import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.idgen.UuidV7;

/**
 * UUID v7 实现（RFC 9562）— 时间有序 + 随机后缀
 * <p>
 * 算法实现位于 common 模块 {@link UuidV7}，领域事件在聚合根内直接使用静态方法生成；
 * 本类作为 {@link IdGenerator} Port 的适配器，供应用层注入实体 ID。
 */
public class UuidV7IdGenerator implements IdGenerator {

    @Override
    public String generateId() {
        return UuidV7.generateId();
    }
}
