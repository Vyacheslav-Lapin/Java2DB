package common;

import common.functions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static common.functions.ExceptionalConsumer.toUncheckedConsumer;

@FunctionalInterface
public interface JdbcDao extends Supplier<Connection> {

    default <T> ExceptionalSupplier<T, SQLException> mapConnection(
            ExceptionalFunction<Connection, T, SQLException> connectionMapper) {
        return () -> {
            try (Connection connection = get()) {
                return connectionMapper.get(connection);
            }
        };
    }

    default <T> ExceptionalSupplier<T, SQLException> mapStatement(
            ExceptionalFunction<Statement, T, SQLException> statementMapper) {
        return mapConnection(connection -> {
            try (Statement statement = connection.createStatement()) {
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

    default <T, C extends Collection<T>> Exceptional<C, SQLException> collect(
            String sql,
            ExceptionalFunction<ResultSet, T, SQLException> rowMapper,
            Supplier<C> collectionConstructor) {
        C collection = collectionConstructor.get();
        try {
            mapAndReduceRows(sql, rowMapper, collection::add)
                    .call();
        } catch (SQLException e) {
            return Exceptional.withException(e);
        }
        return Exceptional.withValue(collection);
    }

    default <T> ExceptionalVarFunction<Object, T, SQLException> mapPreparedStatement(
            String preparedSql,
            ExceptionalFunction<PreparedStatement, T, SQLException> preparedStatementMapper) {
        return params -> mapConnection(connection -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(preparedSql)) {
                for (int index = 0; index < params.length; index++)
                    preparedStatement.setObject(index + 1, params[index]);
                return preparedStatementMapper.get(preparedStatement);
            }
        }).call();
    }

    default <T> ExceptionalVarFunction<Object, T, SQLException> mapPreparedResultSet(
            String preparedSql,
            ExceptionalFunction<ResultSet, T, SQLException> preparedResultSetMapper) {
        return mapPreparedStatement(preparedSql, preparedStatement -> {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return preparedResultSetMapper.get(resultSet);
            }
        });
    }

    default <T> ExceptionalVarFunction<Object, Optional<T>, SQLException> mapPreparedRow(
            String preparedSql,
            ExceptionalFunction<ResultSet, T, SQLException> rowMapper) {
        return mapPreparedResultSet(preparedSql,
                resultSet -> resultSet.next() ? Optional.of(rowMapper.get(resultSet)) : Optional.empty());
    }

    default ExceptionalVarConsumer<Object, SQLException> mapPreparedRows(
            String preparedSql,
            ExceptionalConsumer<ResultSet, SQLException> rowMapper) {
        return mapPreparedResultSet(preparedSql, resultSet -> {
            while (resultSet.next())
                rowMapper.call(resultSet);
            return 0;
        })::executeOrThrowUnchecked;
    }

    default <T> ExceptionalVarConsumer<Object, SQLException> mapAndReducePreparedRows(
            String preparedSql,
            ExceptionalFunction<ResultSet, T, SQLException> rowMapper,
            Consumer<T> reducer) {
        return mapPreparedRows(preparedSql, resultSet -> reducer.accept(rowMapper.get(resultSet)));
    }

    default <T, C extends Collection<T>> ExceptionalVarFunction<Object, C, SQLException> preparedCollect(
            String preparedSql,
            ExceptionalFunction<ResultSet, T, SQLException> rowMapper,
            Supplier<C> collectionConstructor) {
        return params -> {
            C collection = collectionConstructor.get();
            mapAndReducePreparedRows(preparedSql, rowMapper, collection::add).call(params);
            return collection;
        };
    }

    /**
     * @deprecated use {@link #preparedCollect(String, ExceptionalFunction, Supplier)} method.
     */
    @Deprecated
    default <T> Stream<T> getStream(String sql,
                                    ExceptionalFunction<ResultSet, T, SQLException> rowMapper,
                                    Object... params) {
        return preparedCollect(sql, rowMapper, HashSet::new)
                .getOrThrowUnchecked(params)
                .stream();
    }

    default <T> Collection<T> getObjects(Constructor<T> constructor) {
        String queryString = getQueryString(constructor);
        ExceptionalFunction<ResultSet, T, SQLException> resultSetTSQLExceptionExceptionalFunction = resultSet -> {
            Object[] param = Arrays.stream(constructor.getParameters())
                    .map(Parameter::getName)
                    .map(JdbcDao::toDbName)
                    .map(ExceptionalFunction.toUncheckedFunction(resultSet::getObject))
                    .map(JdbcDao::typeConverter)
                    .toArray();
            return ExceptionalFunction.getOrThrowUnchecked(constructor::newInstance,
                    param);
        };
        return collect(
                queryString,
                resultSetTSQLExceptionExceptionalFunction,
                ArrayList<T>::new)
                .getOrThrowUnchecked();
    }

    static Object typeConverter(Object o) {
        return o instanceof Date ? ((Date) o).toLocalDate() : o;
    }

    static String getQueryString(Executable executable) {
        String typeName = executable.getAnnotatedReturnType().getType().getTypeName();
        return "SELECT "
                + Arrays.stream(executable.getParameters())
                .map(Parameter::getName)
                .map(JdbcDao::toDbName)
                .collect(Collectors.joining(", "))
                + " FROM "
                + typeName.substring(typeName.lastIndexOf('.') + 1);
    }

    static String toDbName(String name) {
        return name.replaceAll("[A-Z]", "_$0").toLowerCase();
    }

    static String toCamelCase(String name) {
        String[] words = name.split("_");
        return words.length == 1 ? name : words[0]
                + Arrays.stream(Arrays.copyOfRange(words, 1, words.length))
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining());
    }
}