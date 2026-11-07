package com.test.agent.mcp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.*;
import java.util.*;

class SqlQueryToolTest {
    @Test
    void permitsComplexQueriesAndMetadataButRejectsSideEffects() {
        for (String sql :
                List.of(
                        "SELECT ';DELETE' AS text; -- comment",
                        "SELECT '/*! :=' AS text",
                        "SHOW TABLES",
                        "SHOW COLUMNS FROM foo",
                        "DESCRIBE foo",
                        "EXPLAIN SELECT * FROM foo",
                        "SHOW DATABASES",
                        "WITH x AS (SELECT id FROM a) SELECT x.id, COUNT(*) FROM x JOIN b ON"
                                + " x.id=b.id GROUP BY x.id",
                        "SELECT * FROM a WHERE id IN (SELECT id FROM b) UNION ALL SELECT * FROM"
                                + " a")) {
            assertDoesNotThrow(() -> SqlQueryTool.validateSql(sql), sql);
        }
        for (String sql :
                List.of(
                        "",
                        "SELECT 1; DELETE FROM a",
                        "DELETE FROM a",
                        "UPDATE a SET x=1",
                        "CREATE TABLE x(id INT)",
                        "CALL foo()",
                        "SET @a=1",
                        "SELECT * FROM a FOR UPDATE",
                        "SELECT * FROM a LOCK IN SHARE MODE",
                        "SELECT 1 INTO OUTFILE '/tmp/a'",
                        "SELECT 1 INTO @a",
                        "EXPLAIN ANALYZE SELECT 1",
                        "SELECT GET_LOCK('x',1)",
                        "SELECT (SELECT RELEASE_LOCK('x'))",
                        "SELECT @a:=1",
                        "SELECT ?",
                        "WITH x AS (SELECT * FROM a FOR UPDATE) SELECT * FROM x",
                        "SELECT * FROM (SELECT * FROM a FOR UPDATE) x",
                        "SELECT 1 /*! INTO OUTFILE '/tmp/a' */")) {
            assertThrows(IllegalArgumentException.class, () -> SqlQueryTool.validateSql(sql), sql);
        }
    }

    @Test
    void invalidSqlDoesNotAcquireConnection() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        assertThrows(
                IllegalArgumentException.class,
                () -> new SqlQueryTool(jdbc).querySql("DELETE FROM foo"));
        verifyNoInteractions(jdbc);
    }

    @Test
    void preservesDuplicateColumnsNullsTypesAndUsesPreparedStatement() throws Exception {
        Fixture f = new Fixture(1);
        when(f.rs.getObject(1)).thenReturn(null);
        when(f.rs.getObject(2)).thenReturn(new byte[] {1, 2});
        var result = f.tool.querySql("SELECT a,b FROM foo");
        assertEquals(List.of(Arrays.asList(null, "AQI=")), result.get("rows"));
        assertEquals(1, result.get("rowCount"));
        assertEquals(false, result.get("truncated"));
        var columns = (List<?>) result.get("columns");
        assertEquals(columns.get(0), columns.get(1));
        verify(f.statement).setQueryTimeout(10);
        verify(f.statement).setMaxRows(201);
        verify(f.connection).prepareStatement(anyString());
        verify(f.statement).close();
        verify(f.rs).close();
    }

    @Test
    void jdbcDateAndTimestampBecomeStrings() throws Exception {
        Fixture f = new Fixture(1);
        when(f.rs.getObject(1)).thenReturn(java.sql.Date.valueOf("2026-11-07"));
        when(f.rs.getObject(2)).thenReturn(Timestamp.valueOf("2026-11-07 12:34:56"));
        var result = f.tool.querySql("SELECT a,b FROM foo");
        assertEquals(List.of(List.of("2026-11-07", "2026-11-07 12:34:56.0")), result.get("rows"));
    }

    @Test
    void emptyAndExactLimitAreNotTruncatedButExtraRowIs() throws Exception {
        for (int count : new int[] {0, 200, 201}) {
            Fixture f = new Fixture(count);
            var result = f.tool.querySql("SELECT a,b FROM foo");
            assertEquals(Math.min(count, 200), result.get("rowCount"));
            assertEquals(count > 200, result.get("truncated"));
        }
    }

    @Test
    void oversizedResultFailsAndSqlTimeoutPropagatesWithCause() throws Exception {
        Fixture large = new Fixture(1);
        when(large.rs.getObject(1)).thenReturn("中".repeat(400000));
        assertThrows(
                IllegalArgumentException.class, () -> large.tool.querySql("SELECT a,b FROM foo"));
        Fixture timeout = new Fixture(1);
        when(timeout.statement.executeQuery())
                .thenThrow(new SQLTimeoutException("test timeout", "HYT00", 3024));
        assertThrows(
                org.springframework.dao.DataAccessException.class,
                () -> timeout.tool.querySql("SELECT a,b FROM foo"));
        verify(timeout.statement).close();
    }

    private static class Fixture {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        SqlQueryTool tool;

        Fixture(int count) throws Exception {
            when(connection.prepareStatement(anyString())).thenReturn(statement);
            when(statement.executeQuery()).thenReturn(rs);
            var metadata = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(metadata);
            when(metadata.getColumnCount()).thenReturn(2);
            when(metadata.getColumnLabel(anyInt())).thenReturn("same");
            when(metadata.getColumnType(anyInt())).thenReturn(Types.VARCHAR);
            when(metadata.getColumnTypeName(anyInt())).thenReturn("VARCHAR");
            var index = new java.util.concurrent.atomic.AtomicInteger();
            when(rs.next()).thenAnswer(inv -> index.getAndIncrement() < count);
            when(rs.getObject(anyInt())).thenReturn("value");
            tool =
                    new SqlQueryTool(
                            new JdbcTemplate(new SingleConnectionDataSource(connection, true)));
        }
    }
}
