package com.cartethyia.easyorange.framework.mybatis;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CodeEnumTypeHandler} 枚举字符串化守卫测试。
 * <p>
 * 验证 enum ↔ VARCHAR 列的核心转换行为：写入 code 字符串、读取时反查枚举、
 * null 安全、未知 code fail-fast。使用内嵌测试枚举，自包含不依赖业务模块。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CodeEnumTypeHandler 枚举字符串化测试")
class CodeEnumTypeHandlerTest {

    /** 测试用枚举 — 模拟业务枚举的 code/fromCode 模式 */
    enum TestStatus {
        ACTIVE("ACTIVE"),
        DISABLED("DISABLED");

        private final String code;

        TestStatus(String code) { this.code = code; }

        public String getCode() { return code; }

        public static TestStatus fromCode(String code) {
            for (var s : values()) {
                if (s.code.equals(code)) return s;
            }
            throw new IllegalArgumentException("Unknown TestStatus code: " + code);
        }
    }

    private CodeEnumTypeHandler<TestStatus> handler;

    @Mock
    private PreparedStatement ps;

    @Mock
    private ResultSet rs;

    @Mock
    private CallableStatement cs;

    @BeforeEach
    void setUp() {
        // 匿名子类实例化 abstract CodeEnumTypeHandler
        handler = new CodeEnumTypeHandler<>(TestStatus::getCode, TestStatus::fromCode) {};
    }

    @Test
    @DisplayName("setNonNullParameter 把枚举写入为 code 字符串")
    void setNonNullParameter_writesCodeString() throws SQLException {
        handler.setNonNullParameter(ps, 1, TestStatus.ACTIVE, JdbcType.VARCHAR);

        verify(ps).setString(1, "ACTIVE");
    }

    @Test
    @DisplayName("getNullableResult(ResultSet, columnName) 把 code 反查为枚举")
    void getNullableResult_byColumnName_readsEnum() throws SQLException {
        when(rs.getString("status")).thenReturn("DISABLED");

        TestStatus result = handler.getNullableResult(rs, "status");

        assertThat(result).isEqualTo(TestStatus.DISABLED);
    }

    @Test
    @DisplayName("getNullableResult(ResultSet, columnIndex) 把 code 反查为枚举")
    void getNullableResult_byColumnIndex_readsEnum() throws SQLException {
        when(rs.getString(2)).thenReturn("ACTIVE");

        TestStatus result = handler.getNullableResult(rs, 2);

        assertThat(result).isEqualTo(TestStatus.ACTIVE);
    }

    @Test
    @DisplayName("getNullableResult(CallableStatement, columnIndex) 把 code 反查为枚举")
    void getNullableResult_callableStatement_readsEnum() throws SQLException {
        when(cs.getString(1)).thenReturn("DISABLED");

        TestStatus result = handler.getNullableResult(cs, 1);

        assertThat(result).isEqualTo(TestStatus.DISABLED);
    }

    @Test
    @DisplayName("getNullableResult 处理 null 值返回 null")
    void getNullableResult_nullValue_returnsNull() throws SQLException {
        when(rs.getString("status")).thenReturn(null);

        assertThat(handler.getNullableResult(rs, "status")).isNull();
    }

    @Test
    @DisplayName("getNullableResult 未知 code 抛 IllegalArgumentException（fail-fast）")
    void getNullableResult_unknownCode_throws() throws SQLException {
        when(rs.getString("status")).thenReturn("UNKNOWN");

        assertThatThrownBy(() -> handler.getNullableResult(rs, "status"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UNKNOWN");
    }
}
