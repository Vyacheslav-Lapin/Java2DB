package dao;

import model.Contact;
import org.junit.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class StandAlonePlainJdbcContactDaoTest {

    private static ContactDao contactDao =
            StandAlonePlainJdbcContactDao.create("src/test/resources/db/1.sql");

    @Test
    public void findAll() throws Exception {
        List<Contact> contacts = contactDao.findAll();
        assertEquals("[" +
                        "Contact(id=1, firstName=Chris, lastName=Schaefer, birthDate=1981-05-03), " +
                        "Contact(id=2, firstName=Scott, lastName=Tiger, birthDate=1990-11-02), " +
                        "Contact(id=3, firstName=John, lastName=Smith, birthDate=1964-02-28)]",
                contacts.toString());
    }

    @Test
    public void get() throws Exception {
        Contact contact = new Contact(2, "Scott", "Tiger", LocalDate.parse("1990-11-02"));
        assertThat(contactDao.get(2), is(Optional.of(contact)));
    }
}