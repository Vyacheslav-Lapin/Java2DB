package dao;

import common.JdbcDao;
import common.Pool;
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
public class StandAlonePlainJdbcContactDao implements ContactDao, JdbcDao {

    private static final String DRIVER_CLASS_NAME = "org.h2.Driver";
    private static final String JDBC_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

    static {
        ExceptionalRunnable.run(() -> Class.forName(DRIVER_CLASS_NAME));
    }

    private Pool<Connection> connectionPool;

    public StandAlonePlainJdbcContactDao(String... sqlFilePaths) {
        this(new Pool<>(Connection.class,
                        ExceptionalFunction.carryUnchacked(DriverManager::getConnection, JDBC_URL),
                        5),
                sqlFilePaths);
    }

    public StandAlonePlainJdbcContactDao(Pool<Connection> connectionPool, String... sqlFilePaths) {
        this.connectionPool = connectionPool;
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
    }

    @Override
    public List<Contact> findAll() {
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
    public ExceptionalSupplier<Optional<Contact>, SQLException> getQuery(long id) {
        return mapRow(
                "SELECT first_name, last_name, birth_date FROM Contact WHERE id = " + id,
                resultSet -> new Contact(id,
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getDate("birth_date").toLocalDate())
        );
    }

    @Override
    public Connection get() {
        return connectionPool.get();
    }
}
