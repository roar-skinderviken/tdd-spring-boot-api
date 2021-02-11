package no.javatec.hoaxify.hoax;

import no.javatec.hoaxify.TestPage;
import no.javatec.hoaxify.configuration.AppConfiguration;
import no.javatec.hoaxify.error.ApiError;
import no.javatec.hoaxify.file.FileAttachmentRepository;
import no.javatec.hoaxify.file.FileService;
import no.javatec.hoaxify.hoax.vm.HoaxVM;
import no.javatec.hoaxify.shared.GenericResponse;
import no.javatec.hoaxify.user.User;
import no.javatec.hoaxify.user.UserRepository;
import no.javatec.hoaxify.user.UserService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import static no.javatec.hoaxify.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class HoaxControllerTest {

    private static final String API_1_0_HOAXES = "/api/1.0/hoaxes";
    private static final String API_1_0_USERS = "/api/1.0/users";

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    HoaxRepository hoaxRepository;

    @Autowired
    FileAttachmentRepository fileAttachmentRepository;

    @Autowired
    FileService fileService;

    @Autowired
    HoaxService hoaxService;

    @Autowired
    AppConfiguration appConfiguration;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    public void before() throws IOException {
        fileAttachmentRepository.deleteAll();
        hoaxRepository.deleteAll();
        userRepository.deleteAll();
        FileUtils.cleanDirectory(new File(appConfiguration.getFullAttachmentsPath()));
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_receiveOk() {
        var user = userService.save(createValidUser("user1"));

        postHoax(createValidHoax(), user.getUsername())
                .expectStatus().isOk();
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsNotAuthorized_receiveUnauthorized() {
        postHoax(createValidHoax(), null)
                .expectStatus().isUnauthorized();
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsNotAuthorized_receiveApiError() {
        postHoax(createValidHoax(), null)
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getMessage()).isNotNull());
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSavedToDb() {
        var user = userService.save(createValidUser("user1"));
        postHoax(createValidHoax(), user.getUsername());
        assertThat(hoaxRepository.count()).isEqualTo(1);
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSavedToDbWithTimestamp() {
        var user = userService.save(createValidUser("user1"));
        postHoax(createValidHoax(), user.getUsername());

        var inDb = hoaxRepository.findAll().get(0);
        assertThat(inDb.getTimestamp()).isNotNull();
    }

    @Test
    public void postHoax_whenHoaxContentIsNullAndUserIsAuthorized_receiveBadRequest() {
        var user = userService.save(createValidUser("user1"));
        postHoax(new Hoax(), user.getUsername()).expectStatus().isBadRequest();
    }

    @Test
    public void postHoax_whenHoaxContentIsLessThan10CharactersAndUserIsAuthorized_receiveBadRequest() {
        var user = userService.save(createValidUser("user1"));
        var hoax = new Hoax();

        hoax.setContent("a".repeat(9));
        postHoax(hoax, user.getUsername()).expectStatus().isBadRequest();
    }

    @Test
    public void postHoax_whenHoaxContentIs5000CharactersAndUserIsAuthorized_hoaxIsSavedToDb() {
        var user = userService.save(createValidUser("user1"));
        var hoax = new Hoax();

        hoax.setContent("x".repeat(5000));
        postHoax(hoax, user.getUsername());

        assertThat(hoaxRepository.count()).isEqualTo(1);
    }

    @Test
    public void postHoax_whenHoaxContentIsMoreThan5000CharactersAndUserIsAuthorized_receiveBadRequest() {
        var user = userService.save(createValidUser("user1"));
        var hoax = new Hoax();

        hoax.setContent("x".repeat(5001));

        postHoax(hoax, user.getUsername())
                .expectStatus().isBadRequest();
    }

    @Test
    public void postHoax_whenHoaxContentIsNullAndUserIsAuthorized_receiveApiError() {
        var user = userService.save(createValidUser("user1"));

        postHoax(new Hoax(), user.getUsername())
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getValidationErrors().get("content")).isNotNull());
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSavedToDbWithAuthUserInfo() {
        var user = userService.save(createValidUser("user1"));
        postHoax(createValidHoax(), user.getUsername());

        var inDb = hoaxRepository.findAll().get(0);
        assertThat(inDb.getUser().getUsername()).isEqualTo("user1");
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxCanBeAccessedFromUserEntity() {
        var user = userService.save(createValidUser("user1"));
        postHoax(createValidHoax(), user.getUsername());

        var userInDb = entityManagerFactory.createEntityManager().find(User.class, user.getId());
        assertThat(userInDb.getHoaxes().size()).isEqualTo(1);
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_receiveHoaxVM() {
        var user = userService.save(createValidUser("user1"));

        postHoax(createValidHoax(), user.getUsername())
                .expectBody(HoaxVM.class)
                .value(hoaxVM -> assertThat(hoaxVM.getUser().getUsername()).isEqualTo(user.getUsername()));
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_fileAttachmentHoaxRelationIsUpdatedInDatabase() throws IOException {
        var user = userService.save(createValidUser("user1"));
        var multipartFile = createFile();
        var savedFile = fileService.saveAttachment(multipartFile);

        var hoax = createValidHoax();
        hoax.setAttachment(savedFile);

        var response = postHoax(hoax, user.getUsername())
                .expectBody(HoaxVM.class)
                .returnResult()
                .getResponseBody();

        var fileAttachmentInDb = fileAttachmentRepository.findAll().get(0);
        assertThat(fileAttachmentInDb.getHoax().getId()).isEqualTo(Objects.requireNonNull(response).getId());
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxFileAttachmentRelationsIsUpdatedInDatabase() throws IOException {
        var user = userService.save(createValidUser("user1"));
        var multipartFile = createFile();
        var savedFile = fileService.saveAttachment(multipartFile);

        var hoax = createValidHoax();
        hoax.setAttachment(savedFile);

        var response = postHoax(hoax, user.getUsername())
                .expectBody(HoaxVM.class)
                .returnResult()
                .getResponseBody();

        var inDb = hoaxRepository.findById(Objects.requireNonNull(response).getId()).get();
        assertThat(inDb.getAttachment().getId()).isEqualTo(savedFile.getId());
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_receiveHoaxVMWithAttachment() throws IOException {
        var user = userService.save(createValidUser("user1"));
        var multipartFile = createFile();
        var savedFile = fileService.saveAttachment(multipartFile);

        var hoax = createValidHoax();
        hoax.setAttachment(savedFile);

        postHoax(hoax, user.getUsername())
                .expectBody(HoaxVM.class)
                .value(hoaxVM -> assertThat(hoaxVM.getAttachment().getName()).isEqualTo(savedFile.getName()));
    }

    @Test
    public void getHoaxes_whenThereAreNoHoaxes_receiveOk() {
        webTestClient.get()
                .uri(API_1_0_HOAXES)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void getHoaxes_whenThereAreNoHoaxes_receivePageWithZeroItems() {
        webTestClient.get()
                .uri(API_1_0_HOAXES)
                .exchange()
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getTotalElements()).isEqualTo(0));
    }

    @Test
    public void getHoaxes_whenThereAreHoaxes_receivePageWithItems() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(it -> hoaxService.save(user, createValidHoax()));

        webTestClient.get()
                .uri(API_1_0_HOAXES)
                .exchange()
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getTotalElements()).isEqualTo(3));
    }

    @Test
    public void getHoaxes_whenThereAreHoaxes_receivePageWithHoaxVM() {
        var user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());

        webTestClient.get()
                .uri(API_1_0_HOAXES)
                .exchange()
                .expectBody(new ParameterizedTypeReference<TestPage<HoaxVM>>() {
                })
                .value(page -> assertThat(page.getContent().get(0).getUser().getUsername()).isEqualTo(user.getUsername()));
    }

    @Test
    public void getHoaxesOfUser_whenUserExists_receiveOk() {
        var user = userService.save(createValidUser("user1"));

        webTestClient.get()
                .uri(API_1_0_USERS + "/{username}" + "/hoaxes/", user.getUsername())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void getHoaxesOfUser_whenUserNotExists_receiveNotFound() {
        webTestClient.get()
                .uri(API_1_0_USERS + "/{username}" + "/hoaxes/", "unknown-user")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void getHoaxesOfUser_whenUserExists_receivePageWithZeroHoaxes() {
        var user = userService.save(createValidUser("user1"));

        webTestClient.get()
                .uri(API_1_0_USERS + "/{username}" + "/hoaxes/", user.getUsername())
                .exchange()
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getTotalElements()).isEqualTo(0));
    }

    @Test
    public void getHoaxesOfUser_whenUserExistsWithHoaxes_receivePageWithHoaxVM() {
        var user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createValidHoax());

        webTestClient.get()
                .uri(API_1_0_USERS + "/{username}" + "/hoaxes/", user.getUsername())
                .exchange()
                .expectBody(new ParameterizedTypeReference<TestPage<HoaxVM>>() {
                })
                .value(page -> assertThat(page.getContent().get(0).getUser().getUsername()).isEqualTo(user.getUsername()));
    }

    @Test
    public void getHoaxesOfUser_whenUserExistsWithMultipleHoaxes_receivePageWithThreeHoaxes() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(it -> hoaxService.save(user, createValidHoax()));

        webTestClient.get()
                .uri(API_1_0_USERS + "/{username}" + "/hoaxes/", user.getUsername())
                .exchange()
                .expectBody(new ParameterizedTypeReference<TestPage<HoaxVM>>() {
                })
                .value(page -> assertThat(page.getTotalElements()).isEqualTo(3));
    }

    @Test
    public void getHoaxesOfUser_whenMultipleUserExistsWithMultipleHoaxes_receivePageWithThreeHoaxes() {
        var userWithThreeHoaxes = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(userWithThreeHoaxes, createValidHoax()));

        var userWithFiveHoaxes = userService.save(createValidUser("user2"));
        IntStream.rangeClosed(1, 5).forEach(i -> hoaxService.save(userWithFiveHoaxes, createValidHoax()));

        webTestClient.get()
                .uri(API_1_0_USERS + "/{username}" + "/hoaxes/", userWithFiveHoaxes.getUsername())
                .exchange()
                .expectBody(new ParameterizedTypeReference<TestPage<HoaxVM>>() {
                })
                .value(page -> assertThat(page.getTotalElements()).isEqualTo(5));
    }

    @Test
    public void getOldHoaxes_whenThereAreNoHoaxes_receiveOk() {
        getOldHoaxes(5)
                .expectStatus().isOk();
    }

    @Test
    public void getOldHoaxes_whenThereAreHoaxes_receivePageWithItemsBeforeProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createValidHoax()));
        var hoax = hoaxService.save(user, createValidHoax());

        getOldHoaxes(hoax.getId())
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getTotalElements()).isEqualTo(3));
    }

    @Test
    public void getOldHoaxes_whenThereAreHoaxes_receivePageWithHoaxVMBeforeProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createValidHoax()));
        var hoax = hoaxService.save(user, createValidHoax());

        getOldHoaxes(hoax.getId())
                .expectBody(new ParameterizedTypeReference<TestPage<HoaxVM>>() {
                })
                .value(page -> assertThat(page.getContent().get(0).getDate()).isGreaterThan(0));
    }

    @Test
    public void getOldHoaxesOfUser_whenUserExistsAndThereAreNoHoaxes_receiveOk() {
        var user = userService.save(createValidUser("user1"));

        getOldHoaxesOfUser(5, user.getUsername())
                .expectStatus().isOk();
    }

    @Test
    public void getOldHoaxesOfUser_whenUserExistsWithHoaxes_receivePageWithItemsBeforeProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createValidHoax()));

        var hoax = hoaxService.save(user, createValidHoax());

        getOldHoaxesOfUser(hoax.getId(), user.getUsername())
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getTotalElements()).isEqualTo(3));
    }

    @Test
    public void getOldHoaxesOfUser_whenUserExistsWithHoaxes_receivePageWithHoaxVMBeforeProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createValidHoax()));

        var hoax = hoaxService.save(user, createValidHoax());

        getOldHoaxesOfUser(hoax.getId(), user.getUsername())
                .expectBody(new ParameterizedTypeReference<TestPage<HoaxVM>>() {
                })
                .value(page -> assertThat(page.getContent().get(0).getDate()).isGreaterThan(0));
    }

    @Test
    public void getOldHoaxesOfUser_whenUserDoesNotExists_receiveNotFound() {
        getOldHoaxesOfUser(5, "user1")
                .expectStatus().isNotFound();
    }

    @Test
    public void getOldHoaxesOfUser_whenUserExistsWithNoHoaxes_receivePageWithZeroItemBeforeProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createValidHoax()));
        var hoax = hoaxService.save(user, createValidHoax());

        var userWithoutHoaxes = userService.save(createValidUser("user2"));

        getOldHoaxesOfUser(hoax.getId(), userWithoutHoaxes.getUsername())
                .expectBody(new ParameterizedTypeReference<TestPage<Object>>() {
                })
                .value(page -> assertThat(page.getTotalElements()).isEqualTo(0));
    }

    @Test
    public void getNewHoaxes_whenThereAreHoaxes_receiveListOfItemsAfterProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createValidHoax()));
        var hoax = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        getNewHoaxes(hoax.getId())
                .expectBody(new ParameterizedTypeReference<List<Object>>() {
                })
                .value(list -> assertThat(list.size()).isEqualTo(1));
    }

    @Test
    public void getNewHoaxes_whenThereAreHoaxes_receiveListOfHoaxVM() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createValidHoax()));
        var hoax = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        getNewHoaxes(hoax.getId())
                .expectBody(new ParameterizedTypeReference<List<HoaxVM>>() {
                })
                .value(list -> assertThat(list.get(0).getUser().getUsername()).isEqualTo(user.getUsername()));
    }

    @Test
    public void getNewHoaxesOfUser_whenUserExistsAndThereAreNoHoaxes_receiveOk() {
        var user = userService.save(createValidUser("user1"));

        getNewHoaxesOfUser(5, user.getUsername())
                .expectStatus().isOk();
    }

    @Test
    public void getNewHoaxesOfUser_whenUserExistsWithHoaxes_receiveListWithItemsAfterProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createValidHoax()));

        var fourthHoax = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        getNewHoaxesOfUser(fourthHoax.getId(), user.getUsername())
                .expectBody(new ParameterizedTypeReference<List<Object>>() {
                })
                .value(list -> assertThat(list.size()).isEqualTo(1));
    }

    @Test
    public void getNewHoaxesOfUser_whenUserExistsWithHoaxes_receiveListWithHoaxVMAfterProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createValidHoax()));
        var fourthHoax = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        getNewHoaxesOfUser(fourthHoax.getId(), user.getUsername())
                .expectBody(new ParameterizedTypeReference<List<HoaxVM>>() {
                })
                .value(list -> assertThat(list.get(0).getDate()).isGreaterThan(0));
    }

    @Test
    public void getNewHoaxesOfUser_whenUserDoesNotExists_receiveNotFound() {
        getNewHoaxesOfUser(5, "user1")
                .expectStatus().isNotFound();
    }

    @Test
    public void getNewHoaxesOfUser_whenUserExistsWithNoHoaxes_receivePageWithZeroItemAfterProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createValidHoax()));
        var hoax = hoaxService.save(user, createValidHoax());

        var userWithoutHoaxes = userService.save(createValidUser("user2"));

        getNewHoaxesOfUser(hoax.getId(), userWithoutHoaxes.getUsername())
                .expectBody(new ParameterizedTypeReference<List<Object>>() {
                })
                .value(list -> assertThat(list.size()).isEqualTo(0));
    }

    @Test
    public void getNewHoaxCount_whenThereAreHoaxes_receiveCountAfterProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createValidHoax()));
        var fourthHoax = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        getNewHoaxCount(fourthHoax.getId())
                .expectBody(new ParameterizedTypeReference<Map<String, Long>>() {
                })
                .value(map -> assertThat(map.get("count")).isEqualTo(1));
    }

    @Test
    public void getNewHoaxCountOfUser_whenUserExistsWithHoaxes_receiveCountAfterProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createValidHoax()));

        var fourthHoax = hoaxService.save(user, createValidHoax());
        hoaxService.save(user, createValidHoax());

        getNewHoaxCountOfUser(fourthHoax.getId(), user.getUsername())
                .expectBody(new ParameterizedTypeReference<Map<String, Long>>() {
                })
                .value(map -> assertThat(map.get("count")).isEqualTo(1));
    }

    @Test
    public void deleteHoax_whenUserIsUnauthorized_receiveUnauthorized() {
        deleteHoax(5, null)
                .expectStatus().isUnauthorized();
    }

    @Test
    public void deleteHoax_whenUserAuthorized_receiveOk() {
        var user = userService.save(createValidUser("user1"));
        var hoax = createValidHoax();
        hoaxService.save(user, hoax);

        deleteHoax(hoax.getId(), user.getUsername())
                .expectStatus().isOk();
    }

    @Test
    public void deleteHoax_whenUserAuthorized_receiveGenericResponse() {
        var user = userService.save(createValidUser("user1"));
        var hoax = createValidHoax();

        deleteHoax(hoax.getId(), user.getUsername())
                .expectBody(GenericResponse.class)
                .value(response -> assertThat(response.getMessage()).isNotNull());
    }

    @Test
    public void deleteHoax_whenUserAuthorized_hoaxRemovedFromDatabase() {
        var user = userService.save(createValidUser("user1"));
        var hoax = createValidHoax();
        hoaxService.save(user, hoax);

        deleteHoax(hoax.getId(), user.getUsername());

        var inDb = hoaxRepository.findById(hoax.getId());
        assertThat(inDb.isPresent()).isFalse();
    }

    @Test
    public void deleteHoax_whenHoaxIsOwnedByAnotherUser_receiveForbidden() {
        var otherUser = userService.save(createValidUser("user1"));
        var owner = userService.save(createValidUser("user2"));

        var hoax = createValidHoax();
        hoaxService.save(owner, hoax);

        deleteHoax(hoax.getId(), otherUser.getUsername())
                .expectStatus().isForbidden();
    }

    @Test
    public void deleteHoax_whenHoaxNotExist_receiveForbidden() {
        var user = userService.save(createValidUser("user1"));

        deleteHoax(5, user.getUsername())
                .expectStatus().isForbidden();
    }

    @Test
    public void deleteHoax_whenHoaxHasAttachment_attachmentRemovedFromDatabase() throws IOException {
        var user = userService.save(createValidUser("user1"));

        var multipartFile = createFile();
        var savedFile = fileService.saveAttachment(multipartFile);

        var hoax = createValidHoax();
        hoax.setAttachment(savedFile);

        var response = postHoax(hoax, user.getUsername())
                .expectBody(HoaxVM.class)
                .returnResult()
                .getResponseBody();

        var hoaxId = Objects.requireNonNull(response).getId();

        deleteHoax(hoaxId, user.getUsername());
        var optionalAttachment = fileAttachmentRepository.findById(savedFile.getId());

        assertThat(optionalAttachment.isPresent()).isFalse();
    }

    @Test
    public void deleteHoax_whenHoaxHasAttachment_attachmentRemovedFromFileStorage() throws IOException {
        var user = userService.save(createValidUser("user1"));

        var multipartFile = createFile();
        var savedFile = fileService.saveAttachment(multipartFile);

        var hoax = createValidHoax();
        hoax.setAttachment(savedFile);

        var response = postHoax(hoax, user.getUsername())
                .expectBody(HoaxVM.class)
                .returnResult()
                .getResponseBody();

        var hoaxId = Objects.requireNonNull(response).getId();
        deleteHoax(hoaxId, user.getUsername());

        var fileOnDisk = new File(appConfiguration.getFullAttachmentsPath() + "/" + savedFile.getName());
        assertThat(fileOnDisk.exists()).isFalse();
    }

    private WebTestClient.ResponseSpec deleteHoax(long hoaxId, String loggedInUsername) {
        var clientBuilder = webTestClient.delete()
                .uri(API_1_0_HOAXES + "/" + hoaxId);

        if (loggedInUsername != null) {
            clientBuilder.headers(httpHeaders -> httpHeaders.setBasicAuth(loggedInUsername, TEST_PASSWORD));
        }

        return clientBuilder.exchange();
    }

    private WebTestClient.ResponseSpec getNewHoaxCount(long hoaxId) {
        return webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(API_1_0_HOAXES + "/" + hoaxId)
                        .queryParam("direction", "after")
                        .queryParam("count", "true")
                        .build())
                .exchange();
    }

    private WebTestClient.ResponseSpec getNewHoaxCountOfUser(long hoaxId, String username) {
        return webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(API_1_0_USERS + "/" + username + "/hoaxes/" + hoaxId)
                        .queryParam("direction", "after")
                        .queryParam("count", "true")
                        .build())
                .exchange();
    }

    private WebTestClient.ResponseSpec getOldHoaxes(long hoaxId) {
        return webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(API_1_0_HOAXES + "/" + hoaxId)
                        .queryParam("direction", "before")
                        .queryParam("page", "0")
                        .queryParam("size", "5")
                        .queryParam("sort", "id,desc")
                        .build())
                .exchange();
    }

    private WebTestClient.ResponseSpec getNewHoaxes(long hoaxId) {
        return webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(API_1_0_HOAXES + "/" + hoaxId)
                        .queryParam("direction", "after")
                        .queryParam("sort", "id,desc")
                        .build())
                .exchange();
    }

    private WebTestClient.ResponseSpec getOldHoaxesOfUser(long hoaxId, String username) {
        return webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(API_1_0_USERS + "/" + username + "/hoaxes/" + hoaxId)
                        .queryParam("direction", "before")
                        .queryParam("page", "0")
                        .queryParam("size", "5")
                        .queryParam("sort", "id,desc")
                        .build())
                .exchange();
    }

    private WebTestClient.ResponseSpec getNewHoaxesOfUser(long hoaxId, String username) {
        return webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(API_1_0_USERS + "/" + username + "/hoaxes/" + hoaxId)
                        .queryParam("direction", "after")
                        .queryParam("sort", "id,desc")
                        .build())
                .exchange();
    }

    private WebTestClient.ResponseSpec postHoax(Hoax hoax, String loggedInUsername) {
        var clientBuilder = webTestClient.post()
                .uri(API_1_0_HOAXES);

        if (loggedInUsername != null) {
            clientBuilder.headers(httpHeaders -> httpHeaders.setBasicAuth(loggedInUsername, TEST_PASSWORD));
        }

        return clientBuilder
                .bodyValue(hoax)
                .exchange();
    }

    private MultipartFile createFile() throws IOException {
        var imageResource = new ClassPathResource("profile.png");
        return new MockMultipartFile(
                "profile.png",
                FileUtils.readFileToByteArray(imageResource.getFile()));
    }
}