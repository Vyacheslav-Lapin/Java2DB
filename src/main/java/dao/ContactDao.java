package dao;

import model.Contact;

import java.util.Collections;
import java.util.List;

public interface ContactDao {
    default List<Contact> findAll() {
        return Collections.emptyList();
    }
}