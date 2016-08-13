package dao;

import common.JdbcDao;
import common.Pool;
import common.Private;
import common.functions.*;
import model.Contact;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

import static common.functions.ExceptionalConsumer.toUncheckedConsumer;

@SuppressWarnings("WeakerAccess")
@FunctionalInterface
public interface StandAlonePlainJdbcContactDao extends ContactDao, JdbcDao {

    @Private
    String JDBC_DRIVER_CLASS_KEY = "driver";

    @Private
    String JDBC_URL_KEY = "url";

    @Private
    String JDBC_CONNECTION_POOL_SIZE_KEY = "poolSize";

    @Private
    String DB_PROPERTIES_FILE_NAME = "db.properties";

    @Private
    String SQL_FILE_NAME_SUFFIX = ".sql";

    static ContactDao create(String dbFilesFolderPath) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(dbFilesFolderPath + DB_PROPERTIES_FILE_NAME))) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return create(properties, dbFilesFolderPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static ContactDao create(Properties properties, String dbFilesFolderPath) {
        ExceptionalConsumer.call(Class::forName, (String) properties.remove(JDBC_DRIVER_CLASS_KEY));

        String jdbcUrl = (String) properties.remove(JDBC_URL_KEY);
        int jdbcConnectionPoolSize = Integer.parseInt((String) properties.remove(JDBC_CONNECTION_POOL_SIZE_KEY));

        return create(new Pool<>(Connection.class,
                        ExceptionalBiFunction.carryUnchacked(DriverManager::getConnection, jdbcUrl, properties),
                        jdbcConnectionPoolSize),
                dbFilesFolderPath);
    }

    static ContactDao create(Pool<Connection> connectionPool, String dbFilesFolderPath) {
        List<Path> sqls = new ArrayList<>();
        Path path;
        for (int i = 0; (path = Paths.get(dbFilesFolderPath + ++i + SQL_FILE_NAME_SUFFIX)).toFile().exists();)
                sqls.add(path);
        return create(connectionPool, sqls.stream().toArray(Path[]::new));
    }

    static ContactDao create(Pool<Connection> connectionPool, Path... sqlFilePaths) {
        return ((StandAlonePlainJdbcContactDao) connectionPool::get)
                .executeScripts(sqlFilePaths);
    }

    default ContactDao executeScripts(Path... sqlFilePaths) {
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
