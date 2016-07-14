package dao;

import common.ExceptionalRunnable;
import model.Contact;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static common.ExceptionalConsumer.toUncheckedConsumer;
import static common.ExceptionalFunction.toUncheckedFunction;

@SuppressWarnings("WeakerAccess")
public class StandAlonePlainJdbcContactDao implements ContactDao {

    private static final String DRIVER_CLASS_NAME = "org.h2.Driver";
    private static final String JDBC_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

    static {
        ExceptionalRunnable.run(() -> Class.forName(DRIVER_CLASS_NAME));
    }

    public StandAlonePlainJdbcContactDao(String... sqlFilePaths) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             Statement statement = connection.createStatement()) {
            Arrays.stream(sqlFilePaths)
                    .map(Paths::get)
                    .map(toUncheckedFunction(Files::readAllBytes))
                    .map(String::new)
                    .map(s -> s.split(";"))
                    .flatMap(Arrays::stream)
                    .forEach(toUncheckedConsumer(statement::addBatch));
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Contact> findAll() {
        try (Connection connection = DriverManager.getConnection(JDBC_URL);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM Contact")) {
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
}
