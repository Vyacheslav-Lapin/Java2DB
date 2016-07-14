package dao;

import model.Contact;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StandAlonePlainJdbcContactDaoTest {

    private static ContactDao contactDao = new StandAlonePlainJdbcContactDao("src/test/resources/db/1.sql");

    @Test
    public void findAll() throws Exception {
        List<Contact> contacts = contactDao.findAll();
        assertTrue(contacts.size() == 3);
        assertEquals("[" +
                        "Contact(id=1, firstName=Chris, lastName=Schaefer, birthDate=1981-05-03), " +
                        "Contact(id=2, firstName=Scott, lastName=Tiger, birthDate=1990-11-02), " +
                        "Contact(id=3, firstName=John, lastName=Smith, birthDate=1964-02-28)]",
                contacts.toString());
    }

}