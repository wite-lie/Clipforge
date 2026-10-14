package com.clipforge.knowledge.infrastructure.persistence.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

/**
 * JsonbStringTypeHandler（JSONB 字符串类型处理器）。
 *
 * <p>该处理器负责在 Java String（字符串）和 PostgreSQL JSONB（JSON 二进制类型）之间转换。
 */
public class JsonbStringTypeHandler extends BaseTypeHandler<String> {

    /**
     * setNonNullParameter（设置非空参数）用于把 JSON（对象表示格式）字符串写入 PostgreSQL JSONB（JSON 二进制类型）字段。
     *
     * @param ps PreparedStatement（预编译 SQL 语句）
     * @param i 参数位置
     * @param parameter JSON（对象表示格式）字符串
     * @param jdbcType JDBC（Java 数据库连接）类型
     * @throws SQLException 数据库参数设置失败时抛出
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        // 创建 PostgreSQL 专用对象，用于显式声明字段类型。
        PGobject jsonObject = new PGobject();
        // 设置数据库类型为 jsonb，避免 PostgreSQL 把参数当成普通 varchar（可变字符串）。
        jsonObject.setType("jsonb");
        // 设置 JSON（对象表示格式）字符串内容。
        jsonObject.setValue(parameter);
        // 把 PGobject（PostgreSQL 对象）写入预编译 SQL（结构化查询语言）参数。
        ps.setObject(i, jsonObject);
    }

    /**
     * getNullableResult（读取可空结果）用于按列名读取 JSONB（JSON 二进制类型）字段。
     *
     * @param rs ResultSet（结果集）
     * @param columnName 列名
     * @return JSON（对象表示格式）字符串
     * @throws SQLException 数据库读取失败时抛出
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        // 从结果集中按列名读取字符串。
        return rs.getString(columnName);
    }

    /**
     * getNullableResult（读取可空结果）用于按列位置读取 JSONB（JSON 二进制类型）字段。
     *
     * @param rs ResultSet（结果集）
     * @param columnIndex 列位置
     * @return JSON（对象表示格式）字符串
     * @throws SQLException 数据库读取失败时抛出
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        // 从结果集中按列位置读取字符串。
        return rs.getString(columnIndex);
    }

    /**
     * getNullableResult（读取可空结果）用于从存储过程结果中读取 JSONB（JSON 二进制类型）字段。
     *
     * @param cs CallableStatement（可调用 SQL 语句）
     * @param columnIndex 列位置
     * @return JSON（对象表示格式）字符串
     * @throws SQLException 数据库读取失败时抛出
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        // 从存储过程结果中按列位置读取字符串。
        return cs.getString(columnIndex);
    }
}
