package com.cartethyia.easyorange.framework.config.database;

import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UuidTypeHandler Tests")
class UuidTypeHandlerTest {

    private final UuidTypeHandler handler = new UuidTypeHandler();

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private CallableStatement callableStatement;

    @Nested
    @DisplayName("setNonNullParameter")
    class SetNonNullParameterTests {

        @Test
        @DisplayName("should set UUID as string on PreparedStatement")
        void setNonNullParameter_withUuid_shouldSetString() throws SQLException {
            UUID uuid = UUID.randomUUID();

            handler.setNonNullParameter(preparedStatement, 1, uuid, JdbcType.VARCHAR);

            verify(preparedStatement).setString(1, uuid.toString());
        }

        @Test
        @DisplayName("should handle different parameter index")
        void setNonNullParameter_withDifferentIndex_shouldSetCorrectIndex() throws SQLException {
            UUID uuid = UUID.randomUUID();

            handler.setNonNullParameter(preparedStatement, 3, uuid, JdbcType.VARCHAR);

            verify(preparedStatement).setString(3, uuid.toString());
        }
    }

    @Nested
    @DisplayName("getNullableResult from ResultSet by column name")
    class GetNullableResultByColumnNameTests {

        @Test
        @DisplayName("should convert string to UUID")
        void getNullableResult_withValidUuid_shouldReturnUuid() throws SQLException {
            UUID expected = UUID.randomUUID();
            when(resultSet.getString("id")).thenReturn(expected.toString());

            UUID result = handler.getNullableResult(resultSet, "id");

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should return null when column value is null")
        void getNullableResult_withNullValue_shouldReturnNull() throws SQLException {
            when(resultSet.getString("id")).thenReturn(null);

            UUID result = handler.getNullableResult(resultSet, "id");

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getNullableResult from ResultSet by column index")
    class GetNullableResultByColumnIndexTests {

        @Test
        @DisplayName("should convert string to UUID")
        void getNullableResult_withValidUuid_shouldReturnUuid() throws SQLException {
            UUID expected = UUID.randomUUID();
            when(resultSet.getString(1)).thenReturn(expected.toString());

            UUID result = handler.getNullableResult(resultSet, 1);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should return null when column value is null")
        void getNullableResult_withNullValue_shouldReturnNull() throws SQLException {
            when(resultSet.getString(2)).thenReturn(null);

            UUID result = handler.getNullableResult(resultSet, 2);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("getNullableResult from CallableStatement by column index")
    class GetNullableResultFromCallableStatementTests {

        @Test
        @DisplayName("should convert string to UUID")
        void getNullableResult_withValidUuid_shouldReturnUuid() throws SQLException {
            UUID expected = UUID.randomUUID();
            when(callableStatement.getString(1)).thenReturn(expected.toString());

            UUID result = handler.getNullableResult(callableStatement, 1);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("should return null when column value is null")
        void getNullableResult_withNullValue_shouldReturnNull() throws SQLException {
            when(callableStatement.getString(2)).thenReturn(null);

            UUID result = handler.getNullableResult(callableStatement, 2);

            assertThat(result).isNull();
        }
    }
}
