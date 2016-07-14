package dao;

import common.JdbcDao;
import common.functions.ExceptionalRunnable;
import model.Contact;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static common.functions.ExceptionalConsumer.toUncheckedConsumer;
import static common.functions.ExceptionalFunction.toUncheckedFunction;

@SuppressWarnings("WeakerAccess")
public class StandAlonePlainJdbcContactDao implements ContactDao, JdbcDao {

    private static final String DRIVER_CLASS_NAME = "org.h2.Driver";
    private static final String JDBC_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

    static {
        ExceptionalRunnable.run(() -> Class.forName(DRIVER_CLASS_NAME));
    }

    public StandAlonePlainJdbcContactDao(String... sqlFilePaths) {
        mapStatement(statement -> {
            try {
                Arrays.stream(sqlFilePaths)
                        .map(Paths::get)
                        .map(toUncheckedFunction(Files::readAllBytes))
                        .map(String::new)
                        .map(s -> s.split(";"))
                        .flatMap(Arrays::stream)
                        .forEach(toUncheckedConsumer(statement::addBatch));
                return statement.executeBatch();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }).executeOrThrowUnchecked();
    }

    @Override
    public List<Contact> findAll() {
        return mapResultSet(
                "SELECT id, first_name, last_name, birth_date FROM Contact",
                resultSet -> {
                    try {
                        List<Contact> contacts = new ArrayList<>();
                        while (resultSet.next())
                            contacts.add(new Contact(
                                    resultSet.getLong("id"),
                                    resultSet.getString("first_name"),
                                    resultSet.getString("last_name"),
                                    resultSet.getDate("birth_date").toLocalDate()));
                        return contacts;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        ).getOrThrowUnchecked();
    }

    @Override
    public Optional<Contact> get(long id) {
        return mapResultSet(
                "SELECT first_name, last_name, birth_date from Contact",
                resultSet -> !resultSet.next() ? null : new Contact(id,
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getDate("birth_date").toLocalDate())
        ).get().toOptional();
    }

    @Override
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(JDBC_URL);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
