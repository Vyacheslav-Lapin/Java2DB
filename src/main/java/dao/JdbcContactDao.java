package dao;

import common.ConnectionPool;
import common.JdbcDao;
import common.functions.ExceptionalSupplier;
import common.functions.ExceptionalVarFunction;
import model.Contact;

import java.lang.reflect.Constructor;
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
        //noinspection unchecked
        return getObjects((Constructor<Contact>) Contact.class.getConstructors()[0]).stream();
    }

    @Override
    default ExceptionalSupplier<Optional<Contact>, SQLException> getQuery(long id) {
        return ExceptionalVarFunction.supply(
//                get(Contact.class),
                mapPreparedRow(
                        "SELECT first_name, last_name, birth_date FROM Contact WHERE id = ?",
                        resultSet -> new Contact(id,
                                resultSet.getString("first_name"),
                                resultSet.getString("last_name"),
                                resultSet.getDate("birth_date").toLocalDate())),
                id);
    }

//    default <R, E extends SQLException> ExceptionalVarFunction<Object, R, E> get(Class<R> aClass) {
//
//        return params -> {
//            return mapPreparedRow(
//                    "SELECT first_name, last_name, birth_date FROM Contact WHERE id = ?",
//                    resultSet -> new Contact(id,
//                            resultSet.getString("first_name"),
//                            resultSet.getString("last_name"),
//                            resultSet.getDate("birth_date").toLocalDate()))
//        };
//    }
}