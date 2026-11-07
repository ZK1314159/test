package com.test.agent.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.VariableAssignment;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.Node;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.statement.DescribeStatement;
import net.sf.jsqlparser.statement.ExplainStatement;
import net.sf.jsqlparser.statement.ShowColumnsStatement;
import net.sf.jsqlparser.statement.ShowStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.show.ShowTablesStatement;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.*;
import java.util.*;

/** 通用只读执行器：SQL 由调用方提供，业务口径不在 Java 中预设。 */
public class SqlQueryTool {
    public static final int MAX_ROWS = 200;
    public static final int MAX_BYTES = 1024 * 1024;
    private static final Set<String> SIDE_EFFECT_FUNCTIONS =
            Set.of(
                    "GET_LOCK",
                    "RELEASE_LOCK",
                    "RELEASE_ALL_LOCKS",
                    "SLEEP",
                    "BENCHMARK",
                    "LOAD_FILE",
                    "MASTER_POS_WAIT",
                    "SOURCE_POS_WAIT",
                    "WAIT_FOR_EXECUTED_GTID_SET",
                    "SYS_EXEC",
                    "SYS_EVAL");
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper = new ObjectMapper();

    public SqlQueryTool(JdbcTemplate jdbc) {
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    public Map<String, Object> querySql(String sql) {
        String validated = validateSql(sql);
        return jdbc.execute(
                (ConnectionCallback<Map<String, Object>>)
                        connection -> {
                            try (PreparedStatement statement =
                                    connection.prepareStatement(validated)) {
                                statement.setQueryTimeout(10);
                                statement.setMaxRows(MAX_ROWS + 1);
                                try (ResultSet rs = statement.executeQuery()) {
                                    ResultSetMetaData metadata = rs.getMetaData();
                                    List<Map<String, Object>> columns = new ArrayList<>();
                                    for (int i = 1; i <= metadata.getColumnCount(); i++) {
                                        columns.add(
                                                Map.of(
                                                        "name",
                                                        metadata.getColumnLabel(i),
                                                        "jdbcType",
                                                        metadata.getColumnType(i),
                                                        "typeName",
                                                        metadata.getColumnTypeName(i)));
                                    }
                                    List<List<Object>> rows = new ArrayList<>();
                                    boolean truncated = false;
                                    int bytes = jsonSize(columns);
                                    while (rs.next()) {
                                        if (rows.size() == MAX_ROWS) {
                                            truncated = true;
                                            break;
                                        }
                                        List<Object> row = new ArrayList<>();
                                        for (int i = 1; i <= columns.size(); i++) {
                                            row.add(jsonValue(rs.getObject(i)));
                                        }
                                        bytes += jsonSize(row);
                                        if (bytes > MAX_BYTES) {
                                            throw new IllegalArgumentException(
                                                    "查询结果超过 1 MiB，请缩小查询范围或字段");
                                        }
                                        rows.add(row);
                                    }
                                    Map<String, Object> result = new LinkedHashMap<>();
                                    result.put("columns", columns);
                                    result.put("rows", rows);
                                    result.put("rowCount", rows.size());
                                    result.put("truncated", truncated);
                                    jsonSize(result);
                                    return result;
                                }
                            }
                        });
    }

    private Object jsonValue(Object value) throws SQLException {
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof Blob blob) {
            try {
                if (blob.length() > MAX_BYTES) throw new IllegalArgumentException("二进制字段超过结果大小限制");
                return Base64.getEncoder().encodeToString(blob.getBytes(1, (int) blob.length()));
            } finally {
                blob.free();
            }
        }
        if (value instanceof Clob clob) {
            try {
                if (clob.length() > MAX_BYTES) throw new IllegalArgumentException("文本字段超过结果大小限制");
                return clob.getSubString(1, (int) clob.length());
            } finally {
                clob.free();
            }
        }
        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return value;
        }
        // JDBC 日期时间使用数据库返回的当地值，不替换时区。
        return value.toString();
    }

    private int jsonSize(Object value) {
        try {
            int size = mapper.writeValueAsBytes(value).length;
            if (size > MAX_BYTES) throw new IllegalArgumentException("查询结果超过 1 MiB，请缩小查询范围或字段");
            return size;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("查询结果无法序列化", e);
        }
    }

    static String validateSql(String sql) {
        if (sql == null || sql.isBlank()) throw new IllegalArgumentException("sql 必须为非空字符串");
        // MySQL 可执行注释与普通注释语义不同，不允许绕过解析器。
        try {
            var statements = CCJSqlParserUtil.parseStatements(sql);
            if (statements.size() != 1) throw new IllegalArgumentException("每次只允许一条只读 SQL");
            Statement statement = statements.get(0);
            if (!(statement instanceof Select
                    || statement instanceof ShowStatement
                    || statement instanceof ShowTablesStatement
                    || statement instanceof ShowColumnsStatement
                    || statement instanceof DescribeStatement
                    || statement instanceof ExplainStatement)) {
                throw new IllegalArgumentException("仅允许 SELECT、SHOW、DESCRIBE 或 EXPLAIN 只读语句");
            }
            if (statement instanceof ExplainStatement explain
                    && explain.getOption(ExplainStatement.OptionType.ANALYZE) != null) {
                throw new IllegalArgumentException("不允许 EXPLAIN ANALYZE");
            }
            SimpleNode ast = (SimpleNode) CCJSqlParserUtil.parseAST(sql);
            // 4.9 的语法树不为所有变量赋值建立独立节点，补查词法 token；字符串中的 := 不受影响。
            for (var token = ast.jjtGetFirstToken(); token != null; token = token.next) {
                for (var comment = token.specialToken;
                        comment != null;
                        comment = comment.specialToken) {
                    if (comment.image.startsWith("/*!"))
                        throw new IllegalArgumentException("不支持 MySQL 可执行注释");
                }
                if (":=".equals(token.image)) throw new IllegalArgumentException("不允许变量赋值");
                if (token == ast.jjtGetLastToken()) break;
            }
            inspect(ast);
            // 执行解析器生成的 SQL，避免原始注释或未建模语法被数据库额外解释。
            return statement.toString();
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException("SQL 无法解析，请使用支持的 MySQL 单条只读语法", e);
        }
    }

    private static void inspect(Node node) {
        Object value = ((SimpleNode) node).jjtGetValue();
        if (value instanceof PlainSelect select
                && (select.getForMode() != null
                        || select.getForUpdateTable() != null
                        || select.getIntoTempTable() != null
                        || select.getIntoTables() != null && !select.getIntoTables().isEmpty())) {
            throw new IllegalArgumentException("不允许锁定查询或 SELECT INTO");
        }
        if (value instanceof VariableAssignment || value instanceof JdbcParameter) {
            throw new IllegalArgumentException("不允许变量赋值或未绑定占位符");
        }
        if (value instanceof Function function) {
            String name = function.getName().replace("`", "").toUpperCase(Locale.ROOT);
            String unqualified = name.substring(name.lastIndexOf('.') + 1);
            if (SIDE_EFFECT_FUNCTIONS.contains(unqualified)) {
                throw new IllegalArgumentException("不允许有副作用的函数：" + unqualified);
            }
        }
        for (int i = 0; i < node.jjtGetNumChildren(); i++) inspect(node.jjtGetChild(i));
    }
}
