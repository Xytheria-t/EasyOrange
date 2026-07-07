package com.cartethyia.easyorange.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PageRequest} 单元测试
 * <p>
 * 规范化在 setter 和全参构造器中自动完成：
 * <ul>
 *   <li>Jackson 反序列化 → no-args + setters → 自动规整 ✓</li>
 *   <li>子类 {@code super(...)} → 全参构造器 → 自动规整 ✓</li>
 *   <li>Builder 显式设值 → 原样保留（同旧行为）</li>
 * </ul>
 */
@DisplayName("PageRequest Tests")
class PageRequestTest {

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
    @DisplayName("Builder (unchanged behavior)")
    class BuilderBehavior {

        @Test
        @DisplayName("不设值时 pageNum/pageSize 为 null（同旧行为）")
        void builder_withoutExplicitValues_nullByDefault() {
            var req = PageRequest.builder().build();

            assertThat(req.getPageNum()).isNull();
            assertThat(req.getPageSize()).isNull();
            assertThat(req.getSortField()).isNull();
            assertThat(req.getSortDirection()).isNull();
        }

        @Test
        @DisplayName("显式设值原样保留")
        void builder_explicitValues_preserved() {
            var req = PageRequest.builder()
                    .pageNum(2).pageSize(20)
                    .sortField("createTime").sortDirection("asc")
                    .build();

            assertThat(req.getPageNum()).isEqualTo(2);
            assertThat(req.getPageSize()).isEqualTo(20);
            assertThat(req.getSortField()).isEqualTo("createTime");
            assertThat(req.getSortDirection()).isEqualTo("asc");
        }
    }
}
