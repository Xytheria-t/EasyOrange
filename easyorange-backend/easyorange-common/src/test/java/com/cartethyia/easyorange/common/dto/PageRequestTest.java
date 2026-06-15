package com.cartethyia.easyorange.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PageRequest} 单元测试
 *
 * @author cartethyia
 */
@DisplayName("PageRequest Tests")
class PageRequestTest {

    @Nested
    @DisplayName("Normalized Tests")
    class NormalizedTests {

        @Test
        @DisplayName("normalized with null pageNum should default to 1")
        void normalized_nullPageNum_defaultsTo1() {
            // Arrange
            PageRequest pageRequest = new PageRequest(null, 10, null, null);

            // Act
            PageRequest normalized = pageRequest.normalized();

            // Assert
            assertThat(normalized.getPageNum()).isEqualTo(1);
        }

        @Test
        @DisplayName("normalized with null pageSize should default to 10")
        void normalized_nullPageSize_defaultsTo10() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, null, null, null);

            // Act
            PageRequest normalized = pageRequest.normalized();

            // Assert
            assertThat(normalized.getPageSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("normalized with pageNum less than 1 should default to 1")
        void normalized_pageNumLessThan1_defaultsTo1() {
            // Arrange
            PageRequest pageRequest = new PageRequest(0, 10, null, null);

            // Act
            PageRequest normalized = pageRequest.normalized();

            // Assert
            assertThat(normalized.getPageNum()).isEqualTo(1);
        }

        @Test
        @DisplayName("normalized with pageSize exceeds max should cap at 100")
        void normalized_pageSizeExceedsMax_cappedAt100() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, 200, null, null);

            // Act
            PageRequest normalized = pageRequest.normalized();

            // Assert
            assertThat(normalized.getPageSize()).isEqualTo(100);
        }

        @Test
        @DisplayName("normalized with valid values should preserve them")
        void normalized_validValues_preservesValues() {
            // Arrange
            PageRequest pageRequest = new PageRequest(2, 20, "createTime", "asc");

            // Act
            PageRequest normalized = pageRequest.normalized();

            // Assert
            assertThat(normalized.getPageNum()).isEqualTo(2);
            assertThat(normalized.getPageSize()).isEqualTo(20);
            assertThat(normalized.getSortField()).isEqualTo("createTime");
            assertThat(normalized.getSortDirection()).isEqualTo("asc");
        }

        @Test
        @DisplayName("normalized should preserve sortField and sortDirection")
        void normalized_shouldPreserveSortFieldAndDirection() {
            // Arrange
            PageRequest pageRequest = new PageRequest(null, null, "id", "desc");

            // Act
            PageRequest normalized = pageRequest.normalized();

            // Assert
            assertThat(normalized.getSortField()).isEqualTo("id");
            assertThat(normalized.getSortDirection()).isEqualTo("desc");
        }
    }

    @Nested
    @DisplayName("Builder Tests")
    class BuilderTests {

        @Test
        @DisplayName("builder should create PageRequest correctly")
        void builder_shouldCreatePageRequestCorrectly() {
            // Act
            PageRequest pageRequest = PageRequest.builder()
                    .pageNum(2)
                    .pageSize(20)
                    .sortField("id")
                    .sortDirection("desc")
                    .build();

            // Assert
            assertThat(pageRequest.getPageNum()).isEqualTo(2);
            assertThat(pageRequest.getPageSize()).isEqualTo(20);
            assertThat(pageRequest.getSortField()).isEqualTo("id");
            assertThat(pageRequest.getSortDirection()).isEqualTo("desc");
        }

        @Test
        @DisplayName("builder with default values should work")
        void builder_withDefaultValues_shouldWork() {
            // Act
            PageRequest pageRequest = PageRequest.builder().build();

            // Assert
            assertThat(pageRequest.getPageNum()).isNull();
            assertThat(pageRequest.getPageSize()).isNull();
            assertThat(pageRequest.getSortField()).isNull();
            assertThat(pageRequest.getSortDirection()).isNull();
        }
    }
}
