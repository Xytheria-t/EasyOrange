package com.cartethyia.easyorange.framework.config.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cartethyia.easyorange.common.enums.BusinessType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AuditLogProperties Tests")
class AuditLogPropertiesTest {

    private AuditLogProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AuditLogProperties();
    }

    @Nested
    @DisplayName("Default Method Mappings")
    class DefaultMethodMappingsTests {

        @Test
        @DisplayName("every default mapping should have a title and a business type")
        void methodMappings_default_shouldBeComplete() {
            Map<String, AuditLogProperties.MethodMapping> mappings = properties.getMethodMappings();

            assertThat(mappings).isNotEmpty();
            mappings.forEach((prefix, rule) -> {
                assertThat(rule.title()).as("title of '%s'", prefix).isNotBlank();
                assertThat(rule.businessType())
                        .as("businessType of '%s'", prefix)
                        .isNotNull();
            });
        }

        @Test
        @DisplayName("should classify write-op prefixes by their business type")
        void methodMappings_default_shouldClassifyCorrectly() {
            assertThat(properties.findMapping("createProduct"))
                    .contains(new AuditLogProperties.MethodMapping("创建", BusinessType.ADD));
            assertThat(properties.findMapping("updateStatus").orElseThrow().businessType())
                    .isEqualTo(BusinessType.UPDATE);
            assertThat(properties.findMapping("deleteProduct").orElseThrow().businessType())
                    .isEqualTo(BusinessType.DELETE);
            assertThat(properties.findMapping("login").orElseThrow().businessType())
                    .isEqualTo(BusinessType.LOGIN);
            assertThat(properties.findMapping("reportProduct").orElseThrow().businessType())
                    .isEqualTo(BusinessType.UPDATE);
        }

        @Test
        @DisplayName("should match the longest prefix")
        void findMapping_shouldPreferLongestPrefix() {
            var rule = properties.findMapping("unbindUser").orElseThrow();

            assertThat(rule.title()).isEqualTo("解绑");
            assertThat(rule.businessType()).isEqualTo(BusinessType.UPDATE);
        }

        @Test
        @DisplayName("should resolve a Chinese title for prefixes without one previously")
        void findMapping_forBarePrefix_shouldResolveTitle() {
            var rule = properties.findMapping("typingStarted").orElseThrow();

            assertThat(rule.title()).isEqualTo("输入中");
            assertThat(rule.businessType()).isEqualTo(BusinessType.UPDATE);
        }

        @Test
        @DisplayName("should return empty when no prefix matches or methodName is null")
        void findMapping_withoutMatch_shouldBeEmpty() {
            assertThat(properties.findMapping("fetchOrders")).isEmpty();
            assertThat(properties.findMapping(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("getMethodMappings should return unmodifiable map")
        void getMethodMappings_shouldBeUnmodifiable() {
            Map<String, AuditLogProperties.MethodMapping> mappings = properties.getMethodMappings();

            assertThatThrownBy(() -> mappings.put("x", new AuditLogProperties.MethodMapping("X", BusinessType.OTHER)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
