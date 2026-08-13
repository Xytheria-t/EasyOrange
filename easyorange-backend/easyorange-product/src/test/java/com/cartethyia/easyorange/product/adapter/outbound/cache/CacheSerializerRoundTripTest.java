package com.cartethyia.easyorange.product.adapter.outbound.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.product.application.query.dto.ProductVO;
import com.cartethyia.easyorange.product.application.query.readmodel.CategoryReadModel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * 缓存值序列化回归测试 — 与 framework {@code RedisConfig} 完全相同的序列化器配置（JSON + 默认类型信息），
 * 验证 Spring Cache 写入 Redis 的值（商品 VO / 分类列表）可无损往返。
 * <p>
 * 这是本重构的关键假设：若缓存值无法反序列化（如 {@code Optional} 这类 {@code java.*} 包的 final 类型
 * 不携带类型信息），Spring Cache 将静默命中失败、反复回源。此处锁死两类实际缓存值的往返能力。
 */
class CacheSerializerRoundTripTest {

    /** 与 {@code RedisConfig.buildJsonSerializer()} 保持一致的构建方式 */
    private static GenericJacksonJsonRedisSerializer serializer() {
        var typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(typeValidator)
                .build();
    }

    @Test
    @DisplayName("ProductVO 往返无损（字段完整）")
    void productVO_roundTrips() {
        var vo = ProductVO.builder()
                .id("1")
                .title("测试商品")
                .price(new BigDecimal("100"))
                .stock(10)
                .build();
        var serializer = serializer();

        Object restored = serializer.deserialize(serializer.serialize(vo));

        assertThat(restored).isInstanceOf(ProductVO.class);
        ProductVO product = (ProductVO) restored;
        assertThat(product.getId()).isEqualTo("1");
        assertThat(product.getTitle()).isEqualTo("测试商品");
        assertThat(product.getPrice()).isEqualByComparingTo("100");
        assertThat(product.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("分类列表（record 元素）往返无损")
    void categoryList_roundTrips() {
        // 必须用可变 ArrayList：List.of() 是不可变 final 类（java.* 包），序列化器不写类型信息
        var list = new ArrayList<>(List.of(new CategoryReadModel("1", "分类1", null, 1, null, 0, 1, null, 0)));
        var serializer = serializer();

        Object restored = serializer.deserialize(serializer.serialize(list));

        assertThat(restored).isInstanceOf(List.class);
        var categories = (List<?>) restored;
        assertThat(categories).hasSize(1);
        assertThat(categories.get(0)).isInstanceOf(CategoryReadModel.class);
    }
}
