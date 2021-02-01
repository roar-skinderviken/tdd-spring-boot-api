package no.javatec.hoaxify.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static no.javatec.hoaxify.TestUtils.createValidUser;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {

    @Autowired
    TestEntityManager testEntityManager;

    @Autowired
    UserRepository userRepository;

    @Test
    public void findByUsername_whenUserExists_returnUser() {
        testEntityManager.persist(createValidUser());

        var userInDb = userRepository.findByUsername("test-user");
        assertThat(userInDb).isNotNull();
    }

    @Test
    public void findByUsername_whenUserNotExists_returnNull() {
        var userInDb = userRepository.findByUsername("test-user");
        assertThat(userInDb).isNull();
    }
}