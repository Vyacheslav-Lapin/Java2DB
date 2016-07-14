package dao;

import model.Contact;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface ContactDao {
    default Optional<Contact> get(long id) {
        return findAll().stream()
                .filter(contact -> contact.getId() == id)
                .findAny();
    }
    default List<Contact> findAll() {
        return Collections.emptyList();
    }
}