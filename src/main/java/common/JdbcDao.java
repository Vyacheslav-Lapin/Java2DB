package common;

import common.functions.ExceptionalFunction;
import common.functions.ExceptionalRunnable;
import common.functions.ExceptionalSupplier;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public interface JdbcDao {

    Connection getConnection();

    default <T> ExceptionalSupplier<T, SQLException> mapConnection(
            ExceptionalFunction<Connection, T, SQLException> connectionMapper) {
        return () -> {
            try (final Connection connection = getConnection()) {
                return connectionMapper.get(connection);
            }
        };
    }

    default <T> ExceptionalSupplier<T, SQLException> mapStatement(
            ExceptionalFunction<Statement, T, SQLException> statementMapper) {
        return mapConnection(connection -> {
            try (final Statement statement = connection.createStatement()) {
                return statementMapper.get(statement);
            }
        });
    }

    default <T> ExceptionalSupplier<T, SQLException> mapResultSet(
            String sql,
            ExceptionalFunction<ResultSet, T, SQLException> resultSetMapper) {
        return mapStatement(statement -> {
            try (final ResultSet rs = statement.executeQuery(sql)) {
                return resultSetMapper.get(rs);
            }
        });
    }

    default <T> ExceptionalSupplier<Optional<T>, SQLException> mapRow(
            String sql,
            ExceptionalFunction<ResultSet, T, SQLException> rowMapper) {
        return mapResultSet(sql,
                resultSet -> resultSet.next() ? Optional.of(rowMapper.get(resultSet)) : Optional.empty());
    }

    default <T> ExceptionalRunnable<SQLException> mapRows(
            String sql,
            ExceptionalFunction<ResultSet, T, SQLException> rowMapper) {
        return mapResultSet(sql, resultSet -> {
            while (resultSet.next())
                rowMapper.get(resultSet);
            return 0;
        })::executeOrThrowUnchecked;
    }
}