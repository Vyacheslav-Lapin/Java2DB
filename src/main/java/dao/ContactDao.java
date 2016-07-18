package dao;

import common.functions.ExceptionalSupplier;
import model.Contact;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("WeakerAccess")
public interface ContactDao {
    default Optional<Contact> get(long id) {
        return getQuery(id).getOrThrowUnchecked();
    }

    default List<Contact> findAll() {
        return Collections.emptyList();
    }

    default ExceptionalSupplier<Optional<Contact>, SQLException> getQuery(long id) {
        return () -> findAll().stream()
                .filter(contact -> contact.getId() == id)
                .findAny();
    }
}