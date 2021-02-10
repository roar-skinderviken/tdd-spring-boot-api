package no.javatec.hoaxify.user;

import no.javatec.hoaxify.TestPage;
import no.javatec.hoaxify.configuration.AppConfiguration;
import no.javatec.hoaxify.error.ApiError;
import no.javatec.hoaxify.shared.GenericResponse;
import no.javatec.hoaxify.user.vm.UserUpdateVM;
import no.javatec.hoaxify.user.vm.UserVM;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import static no.javatec.hoaxify.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerTest {

    public static final String API_1_0_USERS = "/api/1.0/users";

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

    @Autowired
    AppConfiguration appConfiguration;

    @BeforeEach
    public void cleanup() {
        userRepository.deleteAll();
    }

    @AfterEach
    public void cleanupDirectory() throws IOException {
        FileUtils.cleanDirectory(new File(appConfiguration.getFullProfileImagesPath()));
        FileUtils.cleanDirectory(new File(appConfiguration.getFullAttachmentsPath()));
    }

    @Test
    public void postUser_whenUserIsValid_receiveOk() {
        postUser(createValidUser()).expectStatus().isOk();
    }

    @Test
    public void postUser_whenUserIsValid_userSavedToDatabase() {
        postUser(createValidUser());
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    public void postUser_whenUserIsValid_receiveSuccessMessage() {
        postUser(createValidUser())
                .expectBody(GenericResponse.class)
                .value(response -> assertThat(response.getMessage()).isNotNull());
    }

    @Test
    public void postUser_whenUserIsValid_passwordIsHashedInDatabase() {
        var user = createValidUser();
        postUser(user);

        var userInDb = userRepository.findAll().get(0);
        assertThat(userInDb.getPassword()).isNotEqualTo(user.getPassword());
    }

    // null validation

    @Test
    public void postUser_whenUserHasNullName_receiveBadRequest() {
        var user = createValidUser();
        user.setUsername(null);
        postUser(user).expectStatus().isBadRequest();
    }

    @Test
    public void postUser_whenUserHasNullDisplayName_receiveBadRequest() {
        var user = createValidUser();
        user.setDisplayName(null);
        postUser(user).expectStatus().isBadRequest();
    }

    @Test
    public void postUser_whenUserHasNullPassword_receiveBadRequest() {
        var user = createValidUser();
        user.setPassword(null);
        postUser(user).expectStatus().isBadRequest();
    }

    // min validation

    @Test
    public void postUser_whenUserHasNameWithLessThanRequired_receiveBadRequest() {
        var user = createValidUser();
        user.setUsername("abc");
        postUser(user).expectStatus().isBadRequest();
    }

    @Test
    public void postUser_whenUserHasDisplayNameWithLessThanRequired_receiveBadRequest() {
        var user = createValidUser();
        user.setDisplayName("abc");
        postUser(user).expectStatus().isBadRequest();
    }

    @Test
    public void postUser_whenUserHasPasswordWithLessThanRequired_receiveBadRequest() {
        var user = createValidUser();
        user.setPassword("P4sswd");
        postUser(user).expectStatus().isBadRequest();
    }

    // max validation

    @Test
    public void postUser_whenUserHasNameThatExceedsLength_receiveBadRequest() {
        var user = createValidUser();
        user.setUsername("a".repeat(256));
        postUser(user).expectStatus().isBadRequest();
    }

    @Test
    public void postUser_whenUserHasDisplayNameThatExceedsLength_receiveBadRequest() {
        var user = createValidUser();
        user.setDisplayName("a".repeat(256));
        postUser(user).expectStatus().isBadRequest();
    }

    @Test
    public void postUser_whenUserHasPasswordThatExceedsLength_receiveBadRequest() {
        var user = createValidUser();
        user.setPassword("a".repeat(256));
        postUser(user).expectStatus().isBadRequest();
    }

    @Test
    public void postUser_whenUserHasPasswordWithAllLowercase_receiveBadRequest() {
        var user = createValidUser();
        user.setPassword("a".repeat(8));
        postUser(user).expectStatus().isBadRequest();
    }

    @Test
    public void postUser_whenUserHasPasswordWithAllUppercase_receiveBadRequest() {
        var user = createValidUser();
        user.setPassword("A".repeat(8));
        postUser(user).expectStatus().isBadRequest();
    }

    @Test
    public void postUser_whenUserHasPasswordWithAllNumber_receiveBadRequest() {
        var user = createValidUser();
        user.setPassword("12345678");
        postUser(user).expectStatus().isBadRequest();
    }

    @Test
    public void postUser_whenUserIsInvalid_receiveApiError() {
        postUser(new User())
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getUrl()).isEqualTo(API_1_0_USERS));
    }

    @Test
    public void postUser_whenUserIsInvalid_receiveApiErrorWithValidationErrors() {
        postUser(new User())
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getValidationErrors().size()).isEqualTo(3));
    }

    @Test
    public void postUser_whenUserHasNullUsername_receiveMessageOfNullUsername() {
        var user = createValidUser();
        user.setUsername(null);

        postUser(user)
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getValidationErrors().get("username")).isEqualTo("Username cannot be null"));
    }

    @Test
    public void postUser_whenUserHasNullPassword_receiveGenericMessageOfNullError() {
        var user = createValidUser();
        user.setPassword(null);

        postUser(user)
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getValidationErrors().get("password")).isEqualTo("Cannot be null"));
    }

    @Test
    public void postUser_whenUserHasNameWithLessThanRequired_receiveGenericMessageOfSizeError() {
        var user = createValidUser();
        user.setUsername("abc");

        postUser(user)
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getValidationErrors().get("username"))
                        .isEqualTo("It must have minimum 4 and maximum 255 characters"));
    }

    @Test
    public void postUser_whenUserHasInvalidPasswordPattern_receiveMessageOfPasswordPatternError() {
        var user = createValidUser();
        user.setPassword("alllowercase");

        postUser(user)
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getValidationErrors().get("password"))
                        .isEqualTo("Password must have at least one uppercase, one lowercase letter and one number"));
    }

    @Test
    public void postUser_whenAnotherUserHasSameUsername_receiveBadRequest() {
        userRepository.save(createValidUser());
        postUser(createValidUser()).expectStatus().isBadRequest();
    }

    @Test
    public void postUser_whenAnotherUserHasSameUsername_receiveMessageOfDuplicateUsername() {
        userRepository.save(createValidUser());

        postUser(createValidUser())
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getValidationErrors().get("username"))
                        .isEqualTo("This name is in use"));
    }

    @Test
    public void getUsers_whenThereAreNoUsersInDb_receiveOk() {
        getUsers().expectStatus().isOk();
    }

    @Test
    public void getUsers_whenThereAreNoUsersInDb_receivePageWithZeroItems() {
        getUsers()
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getTotalElements()).isEqualTo(0));
    }

    @Test
    public void getUsers_whenThereIsUserInDb_receivePageWithUser() {
        userRepository.save(createValidUser());
        getUsers()
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getTotalElements()).isEqualTo(1));
    }

    @Test
    public void getUsers_whenThereIsUserInDb_receiveUserWithoutPassword() {
        userRepository.save(createValidUser());
        getUsers()
                .expectBody(new ParameterizedTypeReference<TestPage<Map<String, Object>>>() {
                })
                .value(page -> assertThat(page.getContent().get(0).containsKey("password")).isFalse());
    }

    @Test
    public void getUsers_whenPageWithPageSizeOf3And20UsersInDb_receivePageWith3Users() {
        IntStream.rangeClosed(1, 20).forEach(
                it -> userRepository.save(createValidUser("test-user-" + it))
        );

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(API_1_0_USERS)
                        .queryParam("page", "0")
                        .queryParam("size", "3")
                        .build())
                .exchange()
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getContent().size()).isEqualTo(3));
    }

    @Test
    public void getUsers_whenPageSizeNotProvided_receivePageSizeOf10() {
        getUsers()
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getSize()).isEqualTo(10));
    }

    @Test
    public void getUsers_whenPageSizeIsGreaterThan100_receivePageSizeOf100() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(API_1_0_USERS)
                        .queryParam("page", "0")
                        .queryParam("size", "101")
                        .build())
                .exchange()
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getSize()).isEqualTo(100));
    }

    @Test
    public void getUsers_whenPageSizeIsNegative_receivePageSizeOf10() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(API_1_0_USERS)
                        .queryParam("page", "0")
                        .queryParam("size", "-1")
                        .build())
                .exchange()
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getSize()).isEqualTo(10));
    }

    @Test
    public void getUsers_whenPageIsNegative_receivePageOf0() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(API_1_0_USERS)
                        .queryParam("page", "-1")
                        .queryParam("size", "10")
                        .build())
                .exchange()
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getNumber()).isEqualTo(0));
    }

    @Test
    public void getUsers_whenUserIsLoggedIn_receivePageWithoutLoggedInUser() {
        userService.save(createValidUser(TEST_USERNAME));
        userService.save(createValidUser("user2"));
        userService.save(createValidUser("user3"));

        webTestClient.get()
                .uri(API_1_0_USERS)
                .headers(httpHeaders -> httpHeaders.setBasicAuth(TEST_USERNAME, TEST_PASSWORD))
                .exchange()
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getContent().size()).isEqualTo(2));
    }

    @Test
    public void getUserByUsername_whenUserExists_receiveOk() {
        final String username = "test-user";
        userService.save(createValidUser(username));
        getUser(username).expectStatus().isOk();
    }

    @Test
    public void getUserByUsername_whenUserExists_receiveUserWithoutPassword() {
        final String username = "test-user";
        userService.save(createValidUser(username));

        getUser(username)
                .expectBody(String.class)
                .value(s -> assertThat(s.contains("password")).isFalse());
    }

    @Test
    public void getUserByUsername_whenUserDoesNotExists_receiveNotFound() {
        getUser("unknown-user").expectStatus().isNotFound();
    }

    @Test
    public void getUserByUsername_whenUserDoesNotExists_receiveApiError() {
        getUser("unknown-user")
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getMessage().contains("unknown-user")).isTrue());
    }

    @Test
    public void putUser_whenUnauthorizedUserSendsRequest_receiveUnauthorized() {
        putUser(123, null, null)
                .expectStatus()
                .isUnauthorized();
    }

    @Test
    public void putUser_whenUnauthorizedUserSendsUpdateForAnotherUser_receiveForbidden() {
        var user = userService.save(createValidUser("user1"));
        long anotherUserId = user.getId() + 123;

        putUser(anotherUserId, null, user.getUsername())
                .expectStatus()
                .isForbidden();
    }

    @Test
    public void putUser_whenUnauthorizedUserSendsRequest_receiveApiError() {
        putUser(123, null, null)
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getUrl().contains("users/123")).isTrue());
    }

    @Test
    public void putUser_whenUnauthorizedUserSendsUpdateForAnotherUser_receiveApiError() {
        var user = userService.save(createValidUser("user1"));
        long anotherUserId = user.getId() + 123;

        putUser(anotherUserId, null, user.getUsername())
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getUrl().contains("users/" + anotherUserId)).isTrue());
    }

    @Test
    public void putUser_whenValidRequestFromAuthorizedUser_receiveOK() {
        var user = userService.save(createValidUser("user1"));

        putUser(user.getId(), createValidUserUpdateVM(), user.getUsername())
                .expectStatus().isOk();
    }

    @Test
    public void putUser_whenValidRequestFromAuthorizedUser_displayNameUpdated() {
        var user = userService.save(createValidUser("user1"));
        var updateUser = createValidUserUpdateVM();

        putUser(user.getId(), updateUser, user.getUsername());

        var userInDb = userService.getByUsername(user.getUsername());
        assertThat(userInDb.getDisplayName()).isEqualTo(updateUser.getDisplayName());
    }

    @Test
    public void putUser_whenValidRequestFromAuthorizedUser_receiveUserVMWithUpdatedDisplayName() {
        var user = userService.save(createValidUser("user1"));
        var updateUser = createValidUserUpdateVM();

        putUser(user.getId(), updateUser, user.getUsername())
                .expectBody(UserVM.class)
                .value(userVM -> assertThat(userVM.getDisplayName()).isEqualTo(updateUser.getDisplayName()));
    }

    @Test
    public void putUser_withValidRequestBodyWithSupportedImageFromAuthUser_receiveUserVMWithRandomImageName() throws IOException {
        var user = userService.save(createValidUser("user1"));
        var imageString = readFileToBase64("profile.png");

        var updateUser = createValidUserUpdateVM();
        updateUser.setImage(imageString);

        putUser(user.getId(), updateUser, user.getUsername())
                .expectBody(UserVM.class)
                .value(userVM -> assertThat(userVM.getImage()).isNotEqualTo(updateUser.getImage()));
    }

    @Test
    public void putUser_withValidRequestBodyWithSupportedImageFromAuthUser_imageIsStoredUnderProfileFolder() throws IOException {
        var user = userService.save(createValidUser("user1"));
        var imageString = readFileToBase64("profile.png");

        var updateUser = createValidUserUpdateVM();
        updateUser.setImage(imageString);

        putUser(user.getId(), updateUser, user.getUsername())
                .expectBody(UserVM.class)
                .value(userVM -> {
                    String profilePicturePath = appConfiguration.getFullProfileImagesPath() + "/" + userVM.getImage();
                    assertThat(new File(profilePicturePath).exists()).isTrue();
                });
    }

    @Test
    public void putUser_withInvalidRequestBodyWithNullDisplayNameFromAuthUser_receiveBadRequest() {
        var user = userService.save(createValidUser("user1"));
        var updateUser = new UserUpdateVM();

        putUser(user.getId(), updateUser, user.getUsername())
                .expectStatus().isBadRequest();
    }

    @Test
    public void putUser_withInvalidRequestBodyWithTooShortDisplayNameFromAuthUser_receiveBadRequest() {
        var user = userService.save(createValidUser("user1"));
        var updateUser = new UserUpdateVM("abc");

        putUser(user.getId(), updateUser, user.getUsername())
                .expectStatus().isBadRequest();
    }

    @Test
    public void putUser_withInvalidRequestBodyWithTooLongDisplayNameFromAuthUser_receiveBadRequest() {
        var user = userService.save(createValidUser("user1"));
        var updateUser = new UserUpdateVM("a".repeat(256));

        putUser(user.getId(), updateUser, user.getUsername())
                .expectStatus().isBadRequest();
    }

    @Test
    public void putUser_withValidRequestBodyWithJPGImageFromAuthUser_receiveOk() throws IOException {
        var user = userService.save(createValidUser("user1"));
        var imageString = readFileToBase64("test-jpg.jpg");

        var updateUser = createValidUserUpdateVM();
        updateUser.setImage(imageString);

        putUser(user.getId(), updateUser, user.getUsername())
                .expectStatus().isOk();
    }

    @Test
    public void putUser_withValidRequestBodyWithGIFImageFromAuthUser_receiveBadRequest() throws IOException {
        var user = userService.save(createValidUser("user1"));
        var imageString = readFileToBase64("test-gif.gif");

        var updateUser = createValidUserUpdateVM();
        updateUser.setImage(imageString);

        putUser(user.getId(), updateUser, user.getUsername())
                .expectStatus().isBadRequest();
    }

    @Test
    public void putUser_withValidRequestBodyWithTXTAsImageFromAuthUser_receiveValidationErrorForProfileImage() throws IOException {
        var user = userService.save(createValidUser("user1"));
        var imageString = readFileToBase64("test-txt.txt");

        var updateUser = createValidUserUpdateVM();
        updateUser.setImage(imageString);

        putUser(user.getId(), updateUser, user.getUsername())
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getValidationErrors().get("image"))
                        .isEqualTo("Only PNG and JPG files are allowed"));
    }

    @Test
    public void putUser_withValidRequestBodyWithJPGImageForUserWhoHasImage_removesOldImageFromStorage() throws IOException {
        var user = userService.save(createValidUser("user1"));
        var imageString = readFileToBase64("test-jpg.jpg");

        var updateUser = createValidUserUpdateVM();
        updateUser.setImage(imageString);

        var firstResponse = putUser(user.getId(), updateUser, user.getUsername())
                .expectBody(UserVM.class)
                .returnResult()
                .getResponseBody();

        putUser(user.getId(), updateUser, user.getUsername());

        var storedImage = new File(appConfiguration.getFullProfileImagesPath() + "/" + Objects.requireNonNull(firstResponse).getImage());
        assertThat(storedImage.exists()).isFalse();
    }

    private WebTestClient.ResponseSpec getUser(String username) {
        return webTestClient.get()
                .uri(API_1_0_USERS + "/{username}", username)
                .exchange();
    }

    private WebTestClient.ResponseSpec getUsers() {
        return webTestClient.get()
                .uri(API_1_0_USERS)
                .exchange();
    }

    private WebTestClient.ResponseSpec postUser(User user) {
        return webTestClient.post()
                .uri(API_1_0_USERS)
                .bodyValue(user)
                .exchange();
    }

    private WebTestClient.ResponseSpec putUser(long userId, UserUpdateVM request, String loggedInUsername) {
        var clientBuilder = webTestClient.put()
                .uri(API_1_0_USERS + "/{userId}", userId);

        if (loggedInUsername != null) {
            clientBuilder.headers(httpHeaders -> httpHeaders.setBasicAuth(loggedInUsername, TEST_PASSWORD));
        }

        if (request != null) {
            clientBuilder.bodyValue(request);
        }

        return clientBuilder.exchange();
    }

    private String readFileToBase64(String fileName) throws IOException {
        ClassPathResource imageResource = new ClassPathResource(fileName);
        return Base64.getEncoder().encodeToString(FileUtils.readFileToByteArray(imageResource.getFile()));
    }

    private UserUpdateVM createValidUserUpdateVM() {
        return new UserUpdateVM("newDisplayName");
    }
}
