package com.cartethyia.easyorange.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    @DisplayName("hasData / hasNext / hasPrevious")
    class NavigationTests {

        @Test
        @DisplayName("有数据时 hasData 为 true")
        void hasData_withData_true() {
            PageResult<String> result = PageResult.of(List.of("a"), 1L, 1, 10);
            assertTrue(result.hasData());
        }

        @Test
        @DisplayName("无数据时 hasData 为 false")
        void hasData_noData_false() {
            PageResult<String> result = PageResult.empty(1, 10);
            assertFalse(result.hasData());
        }

        @Test
        @DisplayName("当前页小于总页数时 hasNext 为 true")
        void hasNext_hasNextPage_true() {
            PageResult<String> result = PageResult.of(List.of("a"), 20L, 1, 10);
            assertTrue(result.hasNext());
        }

        @Test
        @DisplayName("最后一页时 hasNext 为 false")
        void hasNext_lastPage_false() {
            PageResult<String> result = PageResult.of(List.of("a"), 10L, 1, 10);
            assertFalse(result.hasNext());
        }

        @Test
        @DisplayName("第一页时 hasPrevious 为 false")
        void hasPrevious_firstPage_false() {
            PageResult<String> result = PageResult.of(List.of("a"), 20L, 1, 10);
            assertFalse(result.hasPrevious());
        }

        @Test
        @DisplayName("第二页时 hasPrevious 为 true")
        void hasPrevious_secondPage_true() {
            PageResult<String> result = PageResult.of(List.of("a"), 20L, 2, 10);
            assertTrue(result.hasPrevious());
        }
    }
}