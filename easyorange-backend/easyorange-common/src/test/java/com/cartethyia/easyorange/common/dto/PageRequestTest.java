package com.cartethyia.easyorange.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("Validate Sort Field Tests")
    class ValidateSortFieldTests {

        @Test
        @DisplayName("validateSortField with null sortField should return null")
        void validateSortField_nullSortField_returnsNull() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, 10, null, null);
            Set<String> allowedFields = Set.of("id", "createTime");

            // Act
            String result = pageRequest.validateSortField(allowedFields);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("validateSortField with blank sortField should return null")
        void validateSortField_blankSortField_returnsNull() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, 10, "   ", null);
            Set<String> allowedFields = Set.of("id", "createTime");

            // Act
            String result = pageRequest.validateSortField(allowedFields);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("validateSortField with allowed field should return field")
        void validateSortField_allowedField_returnsField() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, 10, "createTime", null);
            Set<String> allowedFields = Set.of("id", "createTime", "updateTime");

            // Act
            String result = pageRequest.validateSortField(allowedFields);

            // Assert
            assertThat(result).isEqualTo("createTime");
        }

        @Test
        @DisplayName("validateSortField with disallowed field should return null")
        void validateSortField_disallowedField_returnsNull() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, 10, "price", null);
            Set<String> allowedFields = Set.of("id", "createTime");

            // Act
            String result = pageRequest.validateSortField(allowedFields);

            // Assert
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("validateSortField with empty allowedFields should return null")
        void validateSortField_emptyAllowedFields_returnsNull() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, 10, "id", null);
            Set<String> allowedFields = Set.of();

            // Act
            String result = pageRequest.validateSortField(allowedFields);

            // Assert
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Get Offset Tests")
    class GetOffsetTests {

        @Test
        @DisplayName("getOffset with default values should return 0")
        void getOffset_defaultValues_returns0() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, 10, null, null);

            // Act
            int offset = pageRequest.getOffset();

            // Assert
            assertThat(offset).isEqualTo(0);
        }

        @Test
        @DisplayName("getOffset with page 2 should return pageSize")
        void getOffset_page2_returnsPageSize() {
            // Arrange
            PageRequest pageRequest = new PageRequest(2, 10, null, null);

            // Act
            int offset = pageRequest.getOffset();

            // Assert
            assertThat(offset).isEqualTo(10);
        }

        @Test
        @DisplayName("getOffset with page 3 and size 20 should return 40")
        void getOffset_page3Size20_returns40() {
            // Arrange
            PageRequest pageRequest = new PageRequest(3, 20, null, null);

            // Act
            int offset = pageRequest.getOffset();

            // Assert
            assertThat(offset).isEqualTo(40);
        }

        @Test
        @DisplayName("getOffset with null pageNum should use default")
        void getOffset_nullPageNum_usesDefault() {
            // Arrange
            PageRequest pageRequest = new PageRequest(null, 10, null, null);

            // Act
            int offset = pageRequest.getOffset();

            // Assert
            assertThat(offset).isEqualTo(0);
        }

        @Test
        @DisplayName("getOffset with null pageSize should use default")
        void getOffset_nullPageSize_usesDefault() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, null, null, null);

            // Act
            int offset = pageRequest.getOffset();

            // Assert
            assertThat(offset).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Is Ascending Tests")
    class IsAscendingTests {

        @Test
        @DisplayName("isAsc with null direction should return true")
        void isAsc_nullDirection_returnsTrue() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, 10, null, null);

            // Act
            boolean result = pageRequest.isAsc();

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isAsc with 'asc' should return true")
        void isAsc_asc_returnsTrue() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, 10, null, "asc");

            // Act
            boolean result = pageRequest.isAsc();

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isAsc with 'ASC' should return true")
        void isAsc_ASC_returnsTrue() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, 10, null, "ASC");

            // Act
            boolean result = pageRequest.isAsc();

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("isAsc with 'desc' should return false")
        void isAsc_desc_returnsFalse() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, 10, null, "desc");

            // Act
            boolean result = pageRequest.isAsc();

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("isAsc with 'DESC' should return false")
        void isAsc_DESC_returnsFalse() {
            // Arrange
            PageRequest pageRequest = new PageRequest(1, 10, null, "DESC");

            // Act
            boolean result = pageRequest.isAsc();

            // Assert
            assertThat(result).isFalse();
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
