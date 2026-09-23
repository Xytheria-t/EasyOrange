package com.cartethyia.easyorange.framework.config.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.cartethyia.easyorange.framework.config.cache.RedisCacheConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;

/**
 * Redis JSON 序列化器 roundtrip 守护 — TD-017。
 * <p>
 * 缓存路径（Spring Cache）按 {@link RedisCacheConfig} 的约定以 Object 读根值：
 * 根 List 曾写出无根类型包装的 {@code [{…@class…}]}，读取期待类型包装而恒抛
 * {@code Unexpected token START_OBJECT, expected VALUE_STRING} → 每次读 WARN + 直查 DB。
 * 本测试锁住「根 List / 根 POJO / String / Long 写读对称」，改序列化配置必须先过这里。
 */
@DisplayName("Redis JSON 序列化器根值 roundtrip")
class RedisJsonSerializerRoundtripTest {

    public record SampleItem(String id, String name, int level) {}

    private final GenericJacksonJsonRedisSerializer serializer = new RedisConfig().jsonRedisSerializer();

    @Test
    @DisplayName("根 List<POJO> 写读对称（eo:category:list 形态）")
    void rootList_roundtrip() {
        List<SampleItem> value = List.of(new SampleItem("1", "电子数码", 1), new SampleItem("2", "书籍教材", 1));

        byte[] bytes = serializer.serialize(value);
        Object back = serializer.deserialize(bytes);

        assertThat(back).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        var list = (List<Object>) back;
        assertThat(list).hasSize(2);
        assertThat(list.get(0)).isInstanceOf(SampleItem.class);
        assertThat(list.get(0)).isEqualTo(value.get(0));
    }

    @Test
    @DisplayName("根 POJO 写读对称（eo:product:info 形态）")
    void rootPojo_roundtrip() {
        var value = new SampleItem("10", "iPhone", 2);

        byte[] bytes = serializer.serialize(value);
        Object back = serializer.deserialize(bytes);

        assertThat(back).isEqualTo(value);
    }

    @Test
    @DisplayName("String / Long 根值不带类型包装，读回等值（限流、SMS、幂等标记形态；JSON 数字无类型时 Jackson 归一为 Integer，按数值断言）")
    void scalars_roundtrip() {
        assertThat(serializer.deserialize(serializer.serialize("1"))).isEqualTo("1");
        assertThat(((Number) serializer.deserialize(serializer.serialize(42L))).longValue()).isEqualTo(42L);
    }

    @Test
    @DisplayName("存量无包装格式（TD-017 修复前写入的根 List）读失败是既知形态：由 CacheErrorHandler 降级直查 DB，回写后自愈")
    void legacyUnwrappedRootList_failsRead_untilRewritten() {
        byte[] legacy = ("[{\"@class\":\"" + SampleItem.class.getName() + "\",\"id\":\"1\",\"name\":\"x\"}]")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> serializer.deserialize(legacy)))
                .isInstanceOf(RuntimeException.class);
    }
}
