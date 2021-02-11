package no.javatec.hoaxify.user;

import no.javatec.hoaxify.error.ApiError;
import no.javatec.hoaxify.user.vm.UserVM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static no.javatec.hoaxify.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LoginControllerTest {

    private static final String API_1_0_LOGIN = "/api/1.0/login";

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    public void cleanup() {
        userRepository.deleteAll();
    }

    /**
     * Request without credentials
     */
    @Test
    public void postLogin_withoutUserCredentials_receiveUnauthorized() {
        webTestClient.post()
                .uri(API_1_0_LOGIN)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Emulating incorrect credentials by not having any users in db.
     */
    @Test
    public void postLogin_withIncorrectCredentials_receiveUnauthorized() {
        webTestClient.post()
                .uri(API_1_0_LOGIN)
                .headers(httpHeaders -> httpHeaders.setBasicAuth(TEST_USERNAME, TEST_PASSWORD))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Verify that error is instance of ApiError
     */
    @Test
    public void postLogin_withoutUserCredentials_receiveApiError() {
        webTestClient.post()
                .uri(API_1_0_LOGIN)
                .exchange()
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getUrl()).isEqualTo(API_1_0_LOGIN));
    }

    /**
     * Verify that null properties are not included in error JSON
     */
    @Test
    public void postLogin_withoutUserCredentials_receiveApiErrorWithoutValidationErrors() {
        webTestClient.post()
                .uri(API_1_0_LOGIN)
                .exchange()
                .expectBody(String.class)
                .value(s -> assertThat(s.contains("validationErrors")).isFalse());
    }

    /**
     * Verify that response headers does not contain "WWW-Authenticate"
     */
    @Test
    public void postLogin_withIncorrectCredentials_receiveUnauthorizedWithoutWWWAuth() {
        webTestClient.post()
                .uri(API_1_0_LOGIN)
                .headers(httpHeaders -> httpHeaders.setBasicAuth(TEST_USERNAME, TEST_PASSWORD))
                .exchange()
                .expectHeader().doesNotExist("WWW-Authenticate");
    }

    /**
     * Verify response status 200
     */
    @Test
    public void postLogin_withValidCredentials_receiveOk() {
        userService.save(createValidUser());

        webTestClient.post()
                .uri(API_1_0_LOGIN)
                .headers(httpHeaders -> httpHeaders.setBasicAuth(TEST_USERNAME, TEST_PASSWORD))
                .exchange()
                .expectStatus().isOk();
    }

    /**
     * Verify User#id
     */
    @Test
    public void postLogin_withValidCredentials_receiveLoggedInUserId() {
        var userInDb = userService.save(createValidUser());
        getLoginBodySpec().value(userVM -> assertThat(userVM.getId()).isEqualTo(userInDb.getId()));
    }

    /**
     * Verify User#displayName
     */
    @Test
    public void postLogin_withValidCredentials_receiveLoggedInUsersDisplayName() {
        var userInDb = userService.save(createValidUser());
        getLoginBodySpec().value(userVM -> assertThat(userVM.getDisplayName()).isEqualTo(userInDb.getDisplayName()));
    }

    /**
     * Verify User#username
     */
    @Test
    public void postLogin_withValidCredentials_receiveLoggedInUsersUsername() {
        var userInDb = userService.save(createValidUser());
        getLoginBodySpec().value(userVM -> assertThat(userVM.getUsername()).isEqualTo(userInDb.getUsername()));
    }

    /**
     * Verify that response does not contains password
     */
    @Test
    public void postLogin_withValidCredentials_notReceiveLoggedInUsersPassword() {
        webTestClient.post()
                .uri(API_1_0_LOGIN)
                .headers(httpHeaders -> httpHeaders.setBasicAuth(TEST_USERNAME, TEST_PASSWORD))
                .exchange()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .value(map -> assertThat(map.containsKey("password")).isFalse());
    }

    private WebTestClient.BodySpec<UserVM, ?> getLoginBodySpec() {
        return webTestClient.post()
                .uri(API_1_0_LOGIN)
                .headers(httpHeaders -> httpHeaders.setBasicAuth(TEST_USERNAME, TEST_PASSWORD))
                .exchange()
                .expectBody(UserVM.class);
    }
}
