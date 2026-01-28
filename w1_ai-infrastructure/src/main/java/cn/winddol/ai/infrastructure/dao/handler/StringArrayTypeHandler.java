package cn.winddol.ai.infrastructure.dao.handler;


import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;

/**
 * 处理 PostgreSQL 的 TEXT[] 数组类型与 Java String[] 的转换
 */
@MappedTypes(String[].class)
@MappedJdbcTypes(JdbcType.ARRAY)
public class StringArrayTypeHandler extends BaseTypeHandler<String[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String[] parameter, JdbcType jdbcType) throws SQLException {
        // 关键点：告诉 PgSQL 这是一个 "text" 类型的数组
        Connection conn = ps.getConnection();
        Array array = conn.createArrayOf("text", parameter);
        ps.setArray(i, array);
    }

    @Override
    public String[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return getArray(rs.getArray(columnName));
    }

    @Override
    public String[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return getArray(rs.getArray(columnIndex));
    }

    @Override
    public String[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return getArray(cs.getArray(columnIndex));
    }

    private String[] getArray(Array array) throws SQLException {
        if (array == null) {
            return null;
        }
        try {
            // 将 SQL Array 转回 Java String[]
            return (String[]) array.getArray();
        } finally {
            // 释放资源
            array.free();
        }
    }
}
