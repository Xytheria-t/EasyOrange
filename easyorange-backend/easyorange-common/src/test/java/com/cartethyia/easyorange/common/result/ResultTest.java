package com.cartethyia.easyorange.common.result;

import com.cartethyia.easyorange.common.enums.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result 单元测试
 */
@DisplayName("Result 单元测试")
class ResultTest {

    @Nested
    @DisplayName("success 方法")
    class SuccessTests {

        @Test
        @DisplayName("无参 success")
        void success_noArgs() {
            Result<Void> result = Result.success();
            assertTrue(result.isSuccess());
            assertEquals("A0000", result.code());
            assertNull(result.data());
            assertNotNull(result.timestamp());
        }

        @Test
        @DisplayName("带数据 success")
        void success_withData() {
            Result<String> result = Result.success("hello");
            assertTrue(result.isSuccess());
            assertEquals("hello", result.data());
        }

        @Test
        @DisplayName("带数据和消息 success")
        void success_withDataAndMessage() {
            Result<String> result = Result.success("hello", "操作成功");
            assertTrue(result.isSuccess());
            assertEquals("hello", result.data());
            assertEquals("操作成功", result.message());
        }
    }

    @Nested
    @DisplayName("error 方法")
    class ErrorTests {

        @Test
        @DisplayName("无参 error")
        void error_noArgs() {
            Result<Void> result = Result.error("操作失败");
            assertFalse(result.isSuccess());
            assertEquals("B0001", result.code());
        }

        @Test
        @DisplayName("带消息 error")
        void error_withMessage() {
            Result<Void> result = Result.error("出错了");
            assertFalse(result.isSuccess());
            assertEquals("出错了", result.message());
            assertNull(result.data());
        }

        @Test
        @DisplayName("带错误码 error")
        void error_withResultCode() {
            Result<Void> result = Result.error(ResultCode.UNAUTHORIZED);
            assertFalse(result.isSuccess());
            assertEquals("A0401", result.code());
            assertEquals("未登录", result.message());
        }

        @Test
        @DisplayName("带错误码和消息 error")
        void error_withResultCodeAndMessage() {
            Result<Void> result = Result.error(ResultCode.UNAUTHORIZED, "请先登录");
            assertFalse(result.isSuccess());
            assertEquals("A0401", result.code());
            assertEquals("请先登录", result.message());
        }

        @Test
        @DisplayName("带 code 和消息 error")
        void error_withCodeAndMessage() {
            Result<Void> result = Result.error("A0404", "资源不存在");
            assertFalse(result.isSuccess());
            assertEquals("A0404", result.code());
            assertEquals("资源不存在", result.message());
        }
    }

    @Nested
    @DisplayName("便捷方法")
    class ConvenienceTests {

        @Test
        @DisplayName("isSuccess 判断")
        void isSuccess_check() {
            assertTrue(Result.success().isSuccess());
            assertFalse(Result.error("操作失败").isSuccess());
            assertFalse(Result.error("msg").isSuccess());
        }

        @Test
        @DisplayName("withMessage 复制并修改消息")
        void withMessage_copyAndChange() {
            Result<String> original = Result.success("data");
            Result<String> modified = original.withMessage("新消息");

            assertEquals("data", modified.data());
            assertEquals("新消息", modified.message());
            assertEquals(original.code(), modified.code());
            assertEquals(original.timestamp(), modified.timestamp());
        }
    }

    @Nested
    @DisplayName("builder 一致性")
    class BuilderConsistencyTests {

        @Test
        @DisplayName("所有 success 方法都使用 builder 模式")
        void allSuccessUseBuilder() {
            Result<String> r1 = Result.success();
            Result<String> r2 = Result.success("data");
            Result<String> r3 = Result.success("data", "msg");

            assertNotNull(r1.timestamp());
            assertNotNull(r2.timestamp());
            assertNotNull(r3.timestamp());
        }

        @Test
        @DisplayName("所有 error 方法都使用 builder 模式")
        void allErrorUseBuilder() {
            Result<Void> r1 = Result.error("操作失败");
            Result<Void> r2 = Result.error("msg");
            Result<Void> r3 = Result.error(ResultCode.FAIL);
            Result<Void> r4 = Result.error("C0500", "msg");

            assertNotNull(r1.timestamp());
            assertNotNull(r2.timestamp());
            assertNotNull(r3.timestamp());
            assertNotNull(r4.timestamp());
        }
    }
}