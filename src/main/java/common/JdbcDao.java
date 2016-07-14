package common;

import common.functions.ExceptionalFunction;
import common.functions.ExceptionalSupplier;

import java.sql.Connection;
import java.sql.SQLException;

public interface JdbcDao {

    Connection getConnection();

    default <T> ExceptionalSupplier<T, SQLException> mapConnection(
            ExceptionalFunction<Connection, T, SQLException> connectionMapper) {
        return () -> {
            try (Connection connection = getConnection()) {
                return connectionMapper.apply(connection).getOrThrow();
            }
        };
    }
}