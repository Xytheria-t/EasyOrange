package com.cartethyia.easyorange.common.result;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * PageResult 单元测试
 */
@DisplayName("PageResult 单元测试")
class PageResultTest {

    @Nested
    @DisplayName("of 工厂方法")
    class OfTests {

        @Test
        @DisplayName("正常分页计算")
        void of_normalCalculation() {
            PageResult<String> result = PageResult.of(List.of("a", "b"), 10L, 1, 2);
            assertEquals(2, result.records().size());
            assertEquals(10L, result.total());
            assertEquals(1, result.current());
            assertEquals(2, result.size());
            assertEquals(5, result.pages());
        }

        @Test
        @DisplayName("records 为 null 时返回空列表")
        void of_nullRecords_emptyList() {
            PageResult<String> result = PageResult.of(null, 0L, 1, 10);
            assertNotNull(result.records());
            assertTrue(result.records().isEmpty());
        }

        @Test
        @DisplayName("pageSize 为 0 时 pages 为 0")
        void of_zeroPageSize_zeroPages() {
            PageResult<String> result = PageResult.of(List.of(), 10L, 1, 0);
            assertEquals(0, result.pages());
        }
    }

    @Nested
    @DisplayName("empty 工厂方法")
    class EmptyTests {

        @Test
        @DisplayName("创建空分页结果")
        void empty_createsEmptyResult() {
            PageResult<String> result = PageResult.empty(1, 10);
            assertTrue(result.records().isEmpty());
            assertEquals(0L, result.total());
            assertEquals(1, result.current());
            assertEquals(10, result.size());
            assertEquals(0, result.pages());
        }
    }

    @Nested
    @DisplayName("compact constructor null guard")
    class CompactConstructorTests {

        @Test
        @DisplayName("canonical constructor 传入 null records 也返回空列表")
        void canonicalConstructor_nullRecords_emptyList() {
            PageResult<String> result = new PageResult<>(null, 5L, 1, 10, 1);
            assertNotNull(result.records());
            assertTrue(result.records().isEmpty());
        }
    }
}
