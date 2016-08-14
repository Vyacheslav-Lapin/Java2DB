package common;

import common.functions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static common.functions.ExceptionalConsumer.toUncheckedConsumer;

@FunctionalInterface
public interface JdbcDao extends Supplier<Connection> {

    default <T> ExceptionalSupplier<T, SQLException> mapConnection(
            ExceptionalFunction<Connection, T, SQLException> connectionMapper) {
        return () -> {
            try (final Connection connection = get()) {
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

    default JdbcDao executeScripts(Path... sqlFilePaths) {
        mapStatement(statement -> {
            Arrays.stream(sqlFilePaths)
                    .map(ExceptionalFunction.toUncheckedFunction(Files::readAllBytes))
                    .map(String::new)
                    .map(s -> s.split(";"))
                    .flatMap(Arrays::stream)
                    .forEach(toUncheckedConsumer(statement::addBatch));
            return statement.executeBatch();
        }).executeOrThrowUnchecked();
        return this;
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

    default ExceptionalRunnable<SQLException> mapRows(
            String sql,
            ExceptionalConsumer<ResultSet, SQLException> rowMapper) {
        return mapResultSet(sql, resultSet -> {
            while (resultSet.next())
                rowMapper.call(resultSet);
            return 0;
        })::executeOrThrowUnchecked;
    }

    default <T> ExceptionalRunnable<SQLException> mapAndReduceRows(
            String sql,
            ExceptionalFunction<ResultSet, T, SQLException> rowMapper,
            Consumer<T> reducer) {
        return mapRows(sql, resultSet -> reducer.accept(rowMapper.get(resultSet)));
    }

    default <T> ExceptionalVarFunction<Object, T, SQLException> mapPreparedStatement(
            String preparedSql,
            ExceptionalFunction<ResultSet, T, SQLException> resultSetMapper) {
        return params -> {
            try (final Connection connection = get();
                 final PreparedStatement preparedStatement = connection.prepareStatement(preparedSql)) {
                for (int index = 0; index < params.length; index++)
                    preparedStatement.setObject(index + 1, params[index]);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    return resultSetMapper.get(resultSet);
                }
            }
        };
    }

    default <T> Stream<T> getStream(String sql, ExceptionalFunction<ResultSet, T, SQLException> rowMapper, Object... params) {

        final Logger log = Logger.getLogger(this.getClass().getName());

        Connection[] connectionHolder = new Connection[1];
        PreparedStatement[] preparedStatementHolder = new PreparedStatement[1];
        ResultSet[] resultSetHolder = new ResultSet[1];

        Iterable<T> iterable = () -> new Iterator<T>() {
            private final Logger log = Logger.getLogger(this.getClass().getName());
            private ResultSet resultSet;

            private ResultSet getResultSet() {
                if (resultSet == null)
                    try {
                        log.info("init iterator");
                        preparedStatementHolder[0] =
                                (connectionHolder[0] = get())
                                        .prepareCall(sql);

                        for (int index = 0; index < params.length; index++)
                            preparedStatementHolder[0].setObject(index + 1, params[index]);

                        resultSet =
                                resultSetHolder[0] = preparedStatementHolder[0].executeQuery();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }

                return resultSet;
            }

            @Override
            public boolean hasNext() {
                log.info("hasNext called");
                try {
                    return getResultSet().next();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public T next() {
                log.info("next called");
                return rowMapper.apply(getResultSet()).getOrThrowUnchecked();
            }
        };

        log.info("Starting!");

        return StreamSupport.stream(iterable.spliterator(), false)
                .onClose(() -> {
                    try {
                        log.info("closing resultSet");
                        resultSetHolder[0].close();
                    } catch (SQLException ignored) {
                    } finally {
                        try {
                            log.info("closing preparedStatement");
                            preparedStatementHolder[0].close();
                        } catch (SQLException ignored) {
                        } finally {
                            try {
                                log.info("closing connection");
                                connectionHolder[0].close();
                            } catch (SQLException ignored) {
                            }
                        }
                    }
//                    throw new RuntimeException("marker!");
                });
    }
}