package common;

import common.functions.ExceptionalBiFunction;
import common.functions.ExceptionalConsumer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@FunctionalInterface
public interface ConnectionPool extends JdbcDao {

    String JDBC_DRIVER_CLASS_KEY = "driver";
    String JDBC_URL_KEY = "url";
    String JDBC_CONNECTION_POOL_SIZE_KEY = "poolSize";
    String DB_PROPERTIES_FILE_NAME = "db.properties";
    String SQL_FILE_NAME_SUFFIX = ".sql";

    static JdbcDao create(String dbFilesFolderPath) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(dbFilesFolderPath + DB_PROPERTIES_FILE_NAME))) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return create(properties, dbFilesFolderPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static JdbcDao create(Properties properties, String dbFilesFolderPath) {
        ExceptionalConsumer.call(Class::forName, (String) properties.remove(JDBC_DRIVER_CLASS_KEY));
        String jdbcUrl = (String) properties.remove(JDBC_URL_KEY);
        int size = Integer.parseInt((String) properties.remove(JDBC_CONNECTION_POOL_SIZE_KEY));
        return create(jdbcUrl, properties, size, dbFilesFolderPath);
    }

    static JdbcDao create(String jdbcUrl, Properties properties, int size, String dbFilesFolderPath) {
        return (
                (ConnectionPool) new Pool<>(
                        Connection.class,
                        ExceptionalBiFunction.supplyUnchecked(DriverManager::getConnection, jdbcUrl, properties),
                        size
                )::get
        ).executeScripts(dbFilesFolderPath);
    }

    default JdbcDao executeScripts(String dbFilesFolderPath) {
        List<Path> pathList = new ArrayList<>();
        Path path;
        for (int i = 0; (path = Paths.get(dbFilesFolderPath + ++i + SQL_FILE_NAME_SUFFIX)).toFile().exists(); )
            pathList.add(path);
        return executeScripts(pathList.stream().toArray(Path[]::new));
    }
}