package common;

import common.functions.ExceptionalFunction;
import common.functions.ExceptionalSupplier;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public interface JdbcDao {

    Connection getConnection();

    default <T> ExceptionalSupplier<T, SQLException> mapConnection(
            ExceptionalFunction<Connection, T, SQLException> connectionMapper) {
        return () -> {
            try (final Connection connection = getConnection()) {
                return connectionMapper.apply(connection).getOrThrow();
            }
        };
    }

    default <T> ExceptionalSupplier<T, SQLException> mapStatement(
            ExceptionalFunction<Statement, T, SQLException> statementMapper) {
        return mapConnection(connection -> {
            try (final Statement statement = connection.createStatement()) {
                return statementMapper.apply(statement).getOrThrow();
            }
        });
    }
}