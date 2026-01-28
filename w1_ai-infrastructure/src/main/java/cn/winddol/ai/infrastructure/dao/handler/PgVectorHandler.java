package cn.winddol.ai.infrastructure.dao.handler;

import com.pgvector.PGvector;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PgVectorHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) throws SQLException {
        // 使用 PGvector 包装 float[]
        ps.setObject(i, new PGvector(parameter));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toFloatArray(rs.getString(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toFloatArray(rs.getString(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toFloatArray(cs.getString(columnIndex));
    }

    private float[] toFloatArray(String value) throws SQLException {
        if (value == null) {
            return null;
        }
        // 利用 PGvector 的构造函数解析 Postgres 返回的字符串格式 "[1,2,3]"
        return new PGvector(value).toArray();
    }
}