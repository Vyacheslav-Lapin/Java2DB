package dao;

import common.ConnectionPool;
import common.JdbcDao;
import common.functions.ExceptionalSupplier;
import common.functions.ExceptionalVarFunction;
import model.Contact;

import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
@FunctionalInterface
public interface JdbcContactDao extends ContactDao, JdbcDao {

    static JdbcContactDao create(String dbFilesFolderPath) {
        return ConnectionPool.create(dbFilesFolderPath)::get;
    }

    @Override
    default Stream<Contact> findAll() {
        return getStream("SELECT id, first_name, last_name, birth_date FROM Contact",
                resultSet -> new Contact(
                        resultSet.getLong("id"),
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getDate("birth_date").toLocalDate()));
    }

    @Override
    default ExceptionalSupplier<Optional<Contact>, SQLException> getQuery(long id) {
        return ExceptionalVarFunction.carry(
                mapPreparedStatement(
                        "SELECT first_name, last_name, birth_date FROM Contact WHERE id = ?",
                        resultSet -> !resultSet.next() ? Optional.empty() : Optional.of(new Contact(id,
                                resultSet.getString("first_name"),
                                resultSet.getString("last_name"),
                                resultSet.getDate("birth_date").toLocalDate()))),
                id);
    }
}