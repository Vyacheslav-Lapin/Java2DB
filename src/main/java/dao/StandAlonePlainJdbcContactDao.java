package dao;

import common.JdbcDao;
import common.Pool;
import common.Private;
import common.functions.ExceptionalFunction;
import common.functions.ExceptionalRunnable;
import common.functions.ExceptionalSupplier;
import model.Contact;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static common.functions.ExceptionalConsumer.toUncheckedConsumer;
import static common.functions.ExceptionalFunction.toUncheckedFunction;

@SuppressWarnings("WeakerAccess")
@FunctionalInterface
public interface StandAlonePlainJdbcContactDao extends ContactDao, JdbcDao {

    @Private
    String DRIVER_CLASS_NAME = "org.h2.Driver";

    @Private
    String JDBC_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

    static ContactDao create(String... sqlFilePaths) {
        ExceptionalRunnable.run(() -> Class.forName(DRIVER_CLASS_NAME));
        return create(new Pool<>(Connection.class,
                        ExceptionalFunction.carryUnchacked(DriverManager::getConnection, JDBC_URL),
                        5),
                sqlFilePaths);
    }

    static ContactDao create(Pool<Connection> connectionPool, String... sqlFilePaths) {
        return ((StandAlonePlainJdbcContactDao) connectionPool::get).executeScripts(sqlFilePaths);
    }

    default ContactDao executeScripts(String... sqlFilePaths) {
        mapStatement(statement -> {
            Arrays.stream(sqlFilePaths)
                    .map(Paths::get)
                    .map(toUncheckedFunction(Files::readAllBytes))
                    .map(String::new)
                    .map(s -> s.split(";"))
                    .flatMap(Arrays::stream)
                    .forEach(toUncheckedConsumer(statement::addBatch));
            return statement.executeBatch();
        }).executeOrThrowUnchecked();
        return this;
    }

    @Override
    default List<Contact> findAll() {
        List<Contact> contacts = new ArrayList<>();
        mapAndReduceRows("SELECT id, first_name, last_name, birth_date FROM Contact",
                resultSet -> new Contact(
                        resultSet.getLong("id"),
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getDate("birth_date").toLocalDate()),
                contacts::add
        ).run();
        return contacts;
    }

    @Override
    default ExceptionalSupplier<Optional<Contact>, SQLException> getQuery(long id) {
        return mapRow(
                "SELECT first_name, last_name, birth_date FROM Contact WHERE id = " + id,
                resultSet -> new Contact(id,
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getDate("birth_date").toLocalDate())
        );
    }
}
