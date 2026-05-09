package com.cartethyia.easyorange.framework.config.web;

import com.cartethyia.easyorange.framework.config.async.JacksonConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JacksonConfig - Long精度处理测试")
class WebMvcConfigTest {

    @Nested
    @DisplayName("Jackson 2.x ObjectMapper Long序列化验证")
    class Jackson2LongSerializationTests {

        @Test
        @DisplayName("大整数Long应序列化为String - 防止JavaScript精度丢失")
        void largeLong_shouldSerializeAsString() throws Exception {
            JacksonConfig jacksonConfig = new JacksonConfig();
            ObjectMapper mapper = jacksonConfig.objectMapper();

            long largeId = 311597393252585472L;
            String json = mapper.writeValueAsString(largeId);

            assertThat(json).isEqualTo("\"311597393252585472\"");
        }

        @Test
        @DisplayName("超过MAX_SAFE_INTEGER的Long值序列化后应保持完整精度")
        void longAboveMaxSafeInteger_shouldMaintainPrecision() throws Exception {
            JacksonConfig jacksonConfig = new JacksonConfig();
            ObjectMapper mapper = jacksonConfig.objectMapper();

            record TestEntity(long id, String name) {}
            TestEntity entity = new TestEntity(311597393252585472L, "test-order");
            String json = mapper.writeValueAsString(entity);
            JsonNode root = mapper.readTree(json);

            assertThat(root.get("id").isTextual())
                    .as("Long id字段应为字符串类型以防止JavaScript精度丢失")
                    .isTrue();
            assertThat(root.get("id").asText()).isEqualTo("311597393252585472");

            BigInteger parsedId = new BigInteger(root.get("id").asText());
            BigInteger originalId = BigInteger.valueOf(311597393252585472L);
            assertThat(parsedId).isEqualTo(originalId);
        }

        @Test
        @DisplayName("普通小数值Long也应序列化为String保持一致性")
        void smallLong_shouldAlsoSerializeAsString() throws Exception {
            JacksonConfig jacksonConfig = new JacksonConfig();
            ObjectMapper mapper = jacksonConfig.objectMapper();

            long smallId = 12345L;
            String json = mapper.writeValueAsString(smallId);

            assertThat(json).isEqualTo("\"12345\"");
        }
    }

    @Nested
    @DisplayName("Jackson 3.x JsonMapper Long序列化验证")
    class Jackson3LongSerializationTests {

        private final JsonMapper jsonMapper = new JacksonConfig().jsonMapper();

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
            tools.jackson.databind.JsonNode root = jsonMapper.readTree(json);

            assertThat(root.get("id").isTextual())
                    .as("Long id字段应为字符串类型以防止JavaScript精度丢失")
                    .isTrue();
            assertThat(root.get("id").asText()).isEqualTo("311597393252585472");
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
            tools.jackson.databind.JsonNode root = jsonMapper.readTree(json);

            assertThat(root.get("id").isNull()).isTrue();
        }
    }

    @Nested
    @DisplayName("订单ID精度场景验证")
    class OrderIdPrecisionTests {

        @Test
        @DisplayName("Jackson 3.x 订单创建响应中的ID序列化应与原始值完全一致")
        void orderId_shouldSerializeIdentically_jackson3() throws Exception {
            JsonMapper jsonMapper = new JacksonConfig().jsonMapper();

            record OrderResponse(Long id, String orderNo, int status) {}
            OrderResponse response = new OrderResponse(311597393252585472L, "ORD-20260509-001", 1);

            String json = jsonMapper.writeValueAsString(response);
            tools.jackson.databind.JsonNode root = jsonMapper.readTree(json);

            String serializedId = root.get("id").asText();
            assertThat(serializedId).isEqualTo("311597393252585472");

            long deserializedId = Long.parseLong(serializedId);
            assertThat(deserializedId).isEqualTo(311597393252585472L);
        }

        @Test
        @DisplayName("Jackson 2.x 订单创建响应中的ID序列化应与原始值完全一致")
        void orderId_shouldSerializeIdentically_jackson2() throws Exception {
            JacksonConfig jacksonConfig = new JacksonConfig();
            ObjectMapper mapper = jacksonConfig.objectMapper();

            record OrderResponse(long id, String orderNo, int status) {}
            OrderResponse response = new OrderResponse(311597393252585472L, "ORD-20260509-001", 1);

            String json = mapper.writeValueAsString(response);
            JsonNode root = mapper.readTree(json);

            String serializedId = root.get("id").asText();
            assertThat(serializedId).isEqualTo("311597393252585472");

            long deserializedId = Long.parseLong(serializedId);
            assertThat(deserializedId).isEqualTo(311597393252585472L);
        }

        @Test
        @DisplayName("验证问题ID的精度丢失场景已被修复")
        void problematicId_precisionLossShouldBeFixed() throws Exception {
            JacksonConfig jacksonConfig = new JacksonConfig();
            ObjectMapper mapper = jacksonConfig.objectMapper();

            long originalId = 311597393252585472L;
            String json = mapper.writeValueAsString(originalId);

            String roundTripped = json.replace("\"", "");
            assertThat(roundTripped).isEqualTo("311597393252585472");
            assertThat(roundTripped).isNotEqualTo("311597393252585500");
        }
    }
}
