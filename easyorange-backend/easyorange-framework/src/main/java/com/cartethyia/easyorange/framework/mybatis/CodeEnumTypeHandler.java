package com.cartethyia.easyorange.framework.mybatis;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * 通用枚举类型处理器基类，用于将枚举类型映射为数据库中的字符串值。
 * <p>
 * 子类只需提供枚举类型、编码提取函数和编码反查函数即可，
 * 无需重复实现 {@link BaseTypeHandler} 的四个抽象方法。
 *
 * @param <T> 枚举类型
 */
public abstract class CodeEnumTypeHandler<T extends Enum<T>> extends BaseTypeHandler<T> {

    private final Function<T, String> toCode;
    private final Function<String, T> fromCode;

    protected CodeEnumTypeHandler(Function<T, String> toCode, Function<String, T> fromCode) {
        this.toCode = toCode;
        this.fromCode = fromCode;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, toCode.apply(parameter));
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : fromCode.apply(value);
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : fromCode.apply(value);
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : fromCode.apply(value);
    }
}
