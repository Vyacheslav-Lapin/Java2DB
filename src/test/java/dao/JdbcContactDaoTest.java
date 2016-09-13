package dao;

import common.JdbcDao;
import model.Contact;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JdbcContactDaoTest {

    private static ContactDao contactDao =
            JdbcContactDao.create("src/test/resources/db/");

    @Test
    public void findAll() throws Exception {
        assertThat(
                contactDao.findAll()
                        .map(Contact::toString)
                        .collect(Collectors.joining(", ")),

                is("Contact(id=1, firstName=Chris, lastName=Schaefer, birthDate=1981-05-03), " +
                        "Contact(id=2, firstName=Scott, lastName=Tiger, birthDate=1990-11-02), " +
                        "Contact(id=3, firstName=John, lastName=Smith, birthDate=1964-02-28)"));
    }

    @Test
    public void get() throws Exception {
        Contact contact = new Contact(2, "Scott", "Tiger", LocalDate.parse("1990-11-02"));
        assertThat(contactDao.get(2), is(Optional.of(contact)));
    }

    @Test
    public void selectGeneratedCorrectly() throws Exception {
        assertThat(JdbcDao.getQueryString(Contact.class.getConstructors()[0]),
                is("SELECT id, first_name, last_name, birth_date FROM Contact"));
    }

    @Test
    public void readCorrectlyFieldsNamesFromDB() throws Exception {
        assertThat(JdbcDao.toCamelCase("first_name"), is("firstName"));
    }
}