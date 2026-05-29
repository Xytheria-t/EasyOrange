package com.cartethyia.easyorange.user.adapter.outbound.persistence.typehandler;

import com.cartethyia.easyorange.user.domain.enums.LoginMethod;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(LoginMethod.class)
public class LoginMethodTypeHandler extends BaseTypeHandler<LoginMethod> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LoginMethod parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public LoginMethod getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : LoginMethod.fromCode(value);
    }

    @Override
    public LoginMethod getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : LoginMethod.fromCode(value);
    }

    @Override
    public LoginMethod getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : LoginMethod.fromCode(value);
    }
}
