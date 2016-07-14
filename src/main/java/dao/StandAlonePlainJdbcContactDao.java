package dao;

import model.Contact;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StandAlonePlainJdbcContactDao implements ContactDao {

    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public StandAlonePlainJdbcContactDao(String... sqlFilePaths) {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
             Statement statement = connection.createStatement()) {
            Arrays.stream(sqlFilePaths)
                    .map(sqlFilePath -> Paths.get(sqlFilePath))
                    .map(path -> {
                        try {
                            return Files.readAllBytes(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                    .flatMap(s -> Arrays.stream(s.split(";")))
                    .forEach(sql -> {
                        try {
                            statement.addBatch(sql);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Contact> findAll() {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
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
