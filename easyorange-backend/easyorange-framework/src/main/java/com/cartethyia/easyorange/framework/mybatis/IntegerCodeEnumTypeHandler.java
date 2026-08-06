package com.cartethyia.easyorange.framework.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

/**
 * 通用整型编码枚举 TypeHandler — 用于 DB 字段为 TINYINT/INT 的枚举（如状态码）。
 * <p>
 * 用法：子类通过 {@code @MappedTypes(YourEnum.class)} 自动注册，
 * 构造时传入 {@code Enum::getCode} 和 {@code Enum::fromCode} 方法引用。
 *
 * @param <T> 枚举类型
 */
public abstract class IntegerCodeEnumTypeHandler<T extends Enum<T>> extends BaseTypeHandler<T> {

    private final Function<T, Integer> toCode;
    private final Function<Integer, T> fromCode;

    protected IntegerCodeEnumTypeHandler(Function<T, Integer> toCode, Function<Integer, T> fromCode) {
        this.toCode = toCode;
        this.fromCode = fromCode;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        ps.setInt(i, toCode.apply(parameter));
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int value = rs.getInt(columnName);
        return rs.wasNull() ? null : fromCode.apply(value);
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int value = rs.getInt(columnIndex);
        return rs.wasNull() ? null : fromCode.apply(value);
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int value = cs.getInt(columnIndex);
        return cs.wasNull() ? null : fromCode.apply(value);
    }
}
