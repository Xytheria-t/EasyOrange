package com.cartethyia.easyorange.framework.config.web;

import com.cartethyia.easyorange.framework.config.async.JacksonConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JacksonConfig - Long精度处理测试")
class WebMvcConfigTest {

    private final JsonMapper jsonMapper = new JacksonConfig().jsonMapper();

    @Nested
    @DisplayName("Long序列化验证")
    class LongSerializationTests {

        @Test
        @DisplayName("大整数Long应序列化为String - 防止JavaScript精度丢失")
        void largeLong_shouldSerializeAsString() throws Exception {
            long largeId = 311597393252585472L;
            String json = jsonMapper.writeValueAsString(largeId);

            assertThat(json).isEqualTo("\"311597393252585472\"");
        }

        @Test
        @DisplayName("超过MAX_SAFE_INTEGER的Long值序列化后应保持完整精度")
        void longAboveMaxSafeInteger_shouldMaintainPrecision() throws Exception {
            record TestEntity(Long id, String name) {}
            TestEntity entity = new TestEntity(311597393252585472L, "test-order");
            String json = jsonMapper.writeValueAsString(entity);
            JsonNode root = jsonMapper.readTree(json);

            assertThat(root.get("id").asStringOpt())
                    .as("Long id字段应为字符串类型以防止JavaScript精度丢失")
                    .isPresent();
            assertThat(root.get("id").asString()).isEqualTo("311597393252585472");

            BigInteger parsedId = new BigInteger(root.get("id").asString());
            BigInteger originalId = BigInteger.valueOf(311597393252585472L);
            assertThat(parsedId).isEqualTo(originalId);
        }

        @Test
        @DisplayName("普通小数值Long也应序列化为String保持一致性")
        void smallLong_shouldAlsoSerializeAsString() throws Exception {
            Long smallId = 12345L;
            String json = jsonMapper.writeValueAsString(smallId);

            assertThat(json).isEqualTo("\"12345\"");
        }

        @Test
        @DisplayName("null Long值应序列化为null")
        void nullLong_shouldSerializeAsNull() throws Exception {
            record TestEntity(Long id, String name) {}
            TestEntity entity = new TestEntity(null, "test");
            String json = jsonMapper.writeValueAsString(entity);
            JsonNode root = jsonMapper.readTree(json);

            assertThat(root.get("id").isNull()).isTrue();
        }

        @Test
        @DisplayName("基本类型long也应序列化为String")
        void primitiveLong_shouldSerializeAsString() throws Exception {
            record TestEntity(long id, String name) {}
            TestEntity entity = new TestEntity(311597393252585472L, "test-order");
            String json = jsonMapper.writeValueAsString(entity);
            JsonNode root = jsonMapper.readTree(json);

            assertThat(root.get("id").asStringOpt())
                    .as("基本类型long字段也应为字符串类型")
                    .isPresent();
            assertThat(root.get("id").asString()).isEqualTo("311597393252585472");
        }
    }

    @Nested
    @DisplayName("订单ID精度场景验证")
    class OrderIdPrecisionTests {

        @Test
        @DisplayName("订单创建响应中的ID序列化应与原始值完全一致")
        void orderId_shouldSerializeIdentically() throws Exception {
            record OrderResponse(Long id, String orderNo, int status) {}
            OrderResponse response = new OrderResponse(311597393252585472L, "ORD-20260509-001", 1);

            String json = jsonMapper.writeValueAsString(response);
            JsonNode root = jsonMapper.readTree(json);

            String serializedId = root.get("id").asString();
            assertThat(serializedId).isEqualTo("311597393252585472");

            long deserializedId = Long.parseLong(serializedId);
            assertThat(deserializedId).isEqualTo(311597393252585472L);
        }

        @Test
        @DisplayName("验证问题ID的精度丢失场景已被修复")
        void problematicId_precisionLossShouldBeFixed() throws Exception {
            long originalId = 311597393252585472L;
            String json = jsonMapper.writeValueAsString(originalId);

            String roundTripped = json.replace("\"", "");
            assertThat(roundTripped).isEqualTo("311597393252585472");
            assertThat(roundTripped).isNotEqualTo("311597393252585500");
        }
    }
}
