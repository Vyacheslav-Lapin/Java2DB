package dao;

import common.functions.ExceptionalSupplier;
import model.Contact;

import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("WeakerAccess")
public interface ContactDao {
    default Optional<Contact> get(long id) {
        return getQuery(id).getOrThrowUnchecked();
    }

    default Stream<Contact> findAll() {
        return Stream.empty();
    }

    default ExceptionalSupplier<Optional<Contact>, SQLException> getQuery(long id) {
        return () -> findAll()
                .filter(contact -> contact.getId() == id)
                .findAny();
    }
}