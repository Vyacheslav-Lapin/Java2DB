package dao;

import common.ConnectionPool;
import common.JdbcDao;
import common.functions.ExceptionalSupplier;
import model.Contact;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("WeakerAccess")
@FunctionalInterface
public interface JdbcContactDao extends ContactDao, JdbcDao {

    static JdbcContactDao create(String dbFilesFolderPath) {
        return ConnectionPool.create(dbFilesFolderPath)::get;
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
