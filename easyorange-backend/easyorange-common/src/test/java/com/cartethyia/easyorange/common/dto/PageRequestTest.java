package com.cartethyia.easyorange.common.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link PageRequest} 单元测试
 * <p>
 * 三条构造路径都必须给出合法分页：
 * <ul>
 *   <li>no-args 构造 → 字段初始值 ✓（query-param 绑定一个参数都没传时走这条，曾因 null 拆箱 500）</li>
 *   <li>Jackson 反序列化 → no-args + setters → 自动规整 ✓</li>
 *   <li>子类 {@code super(...)} → 全参构造器 → 自动规整 ✓</li>
 *   <li>Builder → {@code @Builder.Default} → 不设值时取默认值 ✓</li>
 * </ul>
 */
@DisplayName("PageRequest Tests")
class PageRequestTest {

    @Nested
    @DisplayName("No-Args Construction (query-param 绑定路径)")
    class NoArgsConstruction {

        @Test
        @DisplayName("不带任何参数构造 → pageNum=1, pageSize=10，绝不产生 null")
        void noArgs_usesFieldDefaults() {
            var req = new PageRequest();

            assertThat(req.getPageNum()).isEqualTo(1);
            assertThat(req.getPageSize()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Setter Normalization")
    class SetterNormalization {

        @Test
        @DisplayName("setPageNum(null) → 1")
        void setPageNum_null_defaultsTo1() {
            var req = new PageRequest();
            req.setPageNum(null);
            assertThat(req.getPageNum()).isEqualTo(1);
        }

        @Test
        @DisplayName("setPageNum(0) → 1")
        void setPageNum_lessThan1_defaultsTo1() {
            var req = new PageRequest();
            req.setPageNum(0);
            assertThat(req.getPageNum()).isEqualTo(1);
        }

        @Test
        @DisplayName("setPageSize(null) → 10")
        void setPageSize_null_defaultsTo10() {
            var req = new PageRequest();
            req.setPageSize(null);
            assertThat(req.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("setPageSize(200) → 100")
        void setPageSize_exceedsMax_cappedAt100() {
            var req = new PageRequest();
            req.setPageSize(200);
            assertThat(req.getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("合法值原样保留")
        void setter_validValues_preservesValues() {
            var req = new PageRequest();
            req.setPageNum(2);
            req.setPageSize(20);
            assertThat(req.getPageNum()).isEqualTo(2);
            assertThat(req.getPageSize()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("All-Args Constructor Normalization")
    class ConstructorNormalization {

        @Test
        @DisplayName("new PageRequest(null, 10, ...) → pageNum = 1")
        void constructor_nullPageNum_usesDefault() {
            var req = new PageRequest(null, 10, null, null);
            assertThat(req.getPageNum()).isEqualTo(1);
        }

        @Test
        @DisplayName("new PageRequest(0, 200, ...) → pageNum=1, pageSize=100")
        void constructor_invalidValues_normalized() {
            var req = new PageRequest(0, 200, null, null);
            assertThat(req.getPageNum()).isEqualTo(1);
            assertThat(req.getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("new PageRequest(2, 20, ...) → 原样保留")
        void constructor_validValues_preserved() {
            var req = new PageRequest(2, 20, "createTime", "asc");
            assertThat(req.getPageNum()).isEqualTo(2);
            assertThat(req.getPageSize()).isEqualTo(20);
            assertThat(req.getSortField()).isEqualTo("createTime");
            assertThat(req.getSortDirection()).isEqualTo("asc");
        }
    }

    @Nested
    @DisplayName("Builder (默认值兜底)")
    class BuilderBehavior {

        @Test
        @DisplayName("不设值时取字段默认值（1 / 10），不再是 null")
        void builder_withoutExplicitValues_usesDefaults() {
            var req = PageRequest.builder().build();

            assertThat(req.getPageNum()).isEqualTo(1);
            assertThat(req.getPageSize()).isEqualTo(10);
            assertThat(req.getSortField()).isNull();
            assertThat(req.getSortDirection()).isNull();
        }

        @Test
        @DisplayName("显式设值原样保留")
        void builder_explicitValues_preserved() {
            var req = PageRequest.builder()
                    .pageNum(2)
                    .pageSize(20)
                    .sortField("createTime")
                    .sortDirection("asc")
                    .build();

            assertThat(req.getPageNum()).isEqualTo(2);
            assertThat(req.getPageSize()).isEqualTo(20);
            assertThat(req.getSortField()).isEqualTo("createTime");
            assertThat(req.getSortDirection()).isEqualTo("asc");
        }
    }
}
