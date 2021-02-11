package no.javatec.hoaxify;

import no.javatec.hoaxify.hoax.Hoax;
import no.javatec.hoaxify.user.User;

public class TestUtils {

    public static final String TEST_USERNAME = "test-user";
    public static final String TEST_PASSWORD = "P4ssword";

    private TestUtils() {
    }

    public static User createValidUser() {
        var user = new User();
        user.setUsername(TEST_USERNAME);
        user.setDisplayName("test-display");
        user.setImage("profile-image.png");
        user.setPassword(TEST_PASSWORD);
        return user;
    }

    public static User createValidUser(String username) {
        var user = createValidUser();
        user.setUsername(username);
        return user;
    }

    public static Hoax createValidHoax() {
        var hoax = new Hoax();
        hoax.setContent("test content for the test hoax");
        return hoax;
    }
}
