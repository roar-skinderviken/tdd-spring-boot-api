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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
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

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    TestRestTemplate testRestTemplate;

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
        testRestTemplate.getRestTemplate().getInterceptors().clear();
        FileUtils.cleanDirectory(new File(appConfiguration.getFullAttachmentsPath()));
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_receiveOk() {
        var user = userService.save(createValidUser("user1"));

        postHoax(createdValidHoax(), user.getUsername())
                .expectStatus().isOk();
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsNotAuthorized_receiveUnauthorized() {
        postHoax(createdValidHoax(), null)
                .expectStatus().isUnauthorized();
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsNotAuthorized_receiveApiError() {
        postHoax(createdValidHoax(), null)
                .expectBody(ApiError.class)
                .value(apiError -> assertThat(apiError.getMessage()).isNotNull());
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSavedToDb() {
        var user = userService.save(createValidUser("user1"));
        postHoax(createdValidHoax(), user.getUsername());
        assertThat(hoaxRepository.count()).isEqualTo(1);
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSavedToDbWithTimestamp() {
        var user = userService.save(createValidUser("user1"));
        postHoax(createdValidHoax(), user.getUsername());

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

        Hoax hoax = new Hoax();
        hoax.setContent("a".repeat(9));
        postHoax(hoax, user.getUsername()).expectStatus().isBadRequest();
    }

    @Test
    public void postHoax_whenHoaxContentIs5000CharactersAndUserIsAuthorized_hoaxIsSavedToDb() {
        var user = userService.save(createValidUser("user1"));

        Hoax hoax = new Hoax();
        hoax.setContent("x".repeat(5000));
        postHoax(hoax, user.getUsername());

        assertThat(hoaxRepository.count()).isEqualTo(1);
    }

    @Test
    public void postHoax_whenHoaxContentIsMoreThan5000CharactersAndUserIsAuthorized_receiveBadRequest() {
        var user = userService.save(createValidUser("user1"));

        Hoax hoax = new Hoax();
        hoax.setContent("x".repeat(5001));

        postHoax(hoax, user.getUsername()).expectStatus().isBadRequest();
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
        postHoax(createdValidHoax(), user.getUsername());

        var inDb = hoaxRepository.findAll().get(0);
        assertThat(inDb.getUser().getUsername()).isEqualTo("user1");
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxCanBeAccessedFromUserEntity() {
        var user = userService.save(createValidUser("user1"));
        postHoax(createdValidHoax(), user.getUsername());

        var userInDb = entityManagerFactory.createEntityManager().find(User.class, user.getId());
        assertThat(userInDb.getHoaxes().size()).isEqualTo(1);
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_receiveHoaxVM() {
        var user = userService.save(createValidUser("user1"));

        postHoax(createdValidHoax(), user.getUsername())
                .expectBody(HoaxVM.class)
                .value(hoaxVM -> assertThat(hoaxVM.getUser().getUsername()).isEqualTo(user.getUsername()));
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_fileAttachmentHoaxRelationIsUpdatedInDatabase() throws IOException {
        var user = userService.save(createValidUser("user1"));

        MultipartFile file = createFile();
        var savedFile = fileService.saveAttachment(file);

        Hoax hoax = createdValidHoax();
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

        MultipartFile file = createFile();
        var savedFile = fileService.saveAttachment(file);

        Hoax hoax = createdValidHoax();
        hoax.setAttachment(savedFile);

        authenticate2(user.getUsername());

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
        authenticate(user.getUsername());

        MultipartFile file = createFile();
        var savedFile = fileService.saveAttachment(file);

        Hoax hoax = createdValidHoax();
        hoax.setAttachment(savedFile);

        postHoax(hoax, user.getUsername())
                .expectBody(HoaxVM.class)
                .value(hoaxVM -> assertThat(hoaxVM.getAttachment().getName()).isEqualTo(savedFile.getName()));
    }

    private MultipartFile createFile() throws IOException {
        ClassPathResource imageResource = new ClassPathResource("profile.png");
        return new MockMultipartFile("profile.png", FileUtils.readFileToByteArray(imageResource.getFile()));
    }

    @Test
    public void getHoaxes_whenThereAreNoHoaxes_receiveOk() {
        var response = getHoaxesOfUser(new ParameterizedTypeReference<>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getHoaxes_whenThereAreNoHoaxes_receivePageWithZeroItems() {
        var response = getHoaxesOfUser(new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isEqualTo(0);
    }

    @Test
    public void getHoaxes_whenThereAreOneHoaxInDb_receivePageWithOneItem() {
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());
        hoaxService.save(user, createdValidHoax());
        hoaxService.save(user, createdValidHoax());
        hoaxService.save(user, createdValidHoax());

        var response = getHoaxesOfUser(new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isEqualTo(3);
    }

    @Test
    public void getHoaxes_whenThereAreOneHoaxInDb_receivePageWithHoaxVM() {
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());
        hoaxService.save(user, createdValidHoax());

        var response = getHoaxesOfUser(new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getContent().get(0).getUser().getUsername()).isEqualTo("user1");
    }

    @Test
    public void getHoaxesOfUser_whenUserExists_receiveOk() {
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());

        var response = getHoaxesOfUser(user.getUsername(), new ParameterizedTypeReference<>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getHoaxesOfUser_whenUserNotExists_receiveNotFound() {
        var response = getHoaxesOfUser("unknown-user", new ParameterizedTypeReference<>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void getHoaxesOfUser_whenUserExists_receivePageWithZeroHoaxes() {
        var user = userService.save(createValidUser("user1"));
        var response = getHoaxesOfUser(user.getUsername(), new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isEqualTo(0);
    }

    @Test
    public void getHoaxesOfUser_whenUserExistsWithHoaxes_receivePageWithHoaxVM() {
        var user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createdValidHoax());

        var response = getHoaxesOfUser(user.getUsername(), new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getContent().get(0).getUser().getUsername()).isEqualTo("user1");
    }

    @Test
    public void getHoaxesOfUser_whenUserExistsWithMultipleHoaxes_receivePageWithThreeHoaxes() {
        var user = userService.save(createValidUser("user1"));
        hoaxService.save(user, createdValidHoax());
        hoaxService.save(user, createdValidHoax());
        hoaxService.save(user, createdValidHoax());

        var response = getHoaxesOfUser(user.getUsername(), new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isEqualTo(3);
    }

    @Test
    public void getHoaxesOfUser_whenMultipleUserExistsWithMultipleHoaxes_receivePageWithThreeHoaxes() {
        var userWithThreeHoaxes = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(userWithThreeHoaxes, createdValidHoax()));

        var userWithFiveHoaxes = userService.save(createValidUser("user2"));
        IntStream.rangeClosed(1, 5).forEach(i -> hoaxService.save(userWithFiveHoaxes, createdValidHoax()));

        var response = getHoaxesOfUser(userWithFiveHoaxes.getUsername(), new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isEqualTo(5);
    }

    @Test
    public void getOldHoaxes_whenThereAreNoHoaxes_receiveOk() {
        var response = getOldHoaxes(5, new ParameterizedTypeReference<>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getOldHoaxes_whenThereAreHoaxes_receivePageWithItemsBeforeProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createdValidHoax()));

        var hoax = hoaxService.save(user, createdValidHoax());

        var response = getOldHoaxes(hoax.getId(), new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isEqualTo(3);
    }

    @Test
    public void getOldHoaxes_whenThereAreHoaxes_receivePageWithHoaxVMBeforeProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createdValidHoax()));

        var hoax = hoaxService.save(user, createdValidHoax());

        var response = getOldHoaxes(hoax.getId(), new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getContent().get(0).getDate()).isGreaterThan(0);
    }

    @Test
    public void getOldHoaxesOfUser_whenUserExistsAndThereAreNoHoaxes_receiveOk() {
        var user = userService.save(createValidUser("user1"));
        var response = getOldHoaxesOfUser(5, user.getUsername(), new ParameterizedTypeReference<>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getOldHoaxesOfUser_whenUserExistsWithHoaxes_receivePageWithItemsBeforeProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createdValidHoax()));

        var hoax = hoaxService.save(user, createdValidHoax());

        var response = getOldHoaxesOfUser(hoax.getId(), user.getUsername(), new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isEqualTo(3);
    }

    @Test
    public void getOldHoaxesOfUser_whenUserExistsWithHoaxes_receivePageWithHoaxVMBeforeProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createdValidHoax()));

        var hoax = hoaxService.save(user, createdValidHoax());

        var response = getOldHoaxesOfUser(hoax.getId(), user.getUsername(), new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getContent().get(0).getDate()).isGreaterThan(0);
    }

    @Test
    public void getOldHoaxesOfUser_whenUserDoesNotExists_receiveNotFound() {
        var response = getOldHoaxesOfUser(5, "user1", new ParameterizedTypeReference<>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void getOldHoaxesOfUser_whenUserExistsWithNoHoaxes_receivePageWithZeroItemBeforeProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createdValidHoax()));
        var hoax = hoaxService.save(user, createdValidHoax());

        var userWithoutHoaxes = userService.save(createValidUser("user2"));

        var response = getOldHoaxesOfUser(hoax.getId(), userWithoutHoaxes.getUsername(), new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).getTotalElements()).isEqualTo(0);
    }

    @Test
    public void getNewHoaxes_whenThereAreHoaxes_receiveListOfItemsAfterProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createdValidHoax()));
        var hoax = hoaxService.save(user, createdValidHoax());
        hoaxService.save(user, createdValidHoax());

        var response = getNewHoaxes(hoax.getId(), new ParameterizedTypeReference<List<Object>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).size()).isEqualTo(1);
    }

    @Test
    public void getNewHoaxes_whenThereAreHoaxes_receiveListOfHoaxVM() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createdValidHoax()));
        var hoax = hoaxService.save(user, createdValidHoax());
        hoaxService.save(user, createdValidHoax());

        var response = getNewHoaxes(hoax.getId(), new ParameterizedTypeReference<List<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).get(0).getUser().getUsername()).isEqualTo(user.getUsername());
    }

    @Test
    public void getNewHoaxesOfUser_whenUserExistsAndThereAreNoHoaxes_receiveOk() {
        var user = userService.save(createValidUser("user1"));
        var response = getNewHoaxesOfUser(5, user.getUsername(), new ParameterizedTypeReference<>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getNewHoaxesOfUser_whenUserExistsWithHoaxes_receiveListWithItemsAfterProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createdValidHoax()));

        var fourthHoax = hoaxService.save(user, createdValidHoax());
        hoaxService.save(user, createdValidHoax());

        var response = getNewHoaxesOfUser(fourthHoax.getId(), user.getUsername(), new ParameterizedTypeReference<List<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).size()).isEqualTo(1);
    }

    @Test
    public void getNewHoaxesOfUser_whenUserExistsWithHoaxes_receiveListWithHoaxVMAfterProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createdValidHoax()));
        var fourthHoax = hoaxService.save(user, createdValidHoax());
        hoaxService.save(user, createdValidHoax());

        var response = getNewHoaxesOfUser(fourthHoax.getId(), user.getUsername(), new ParameterizedTypeReference<List<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).get(0).getDate()).isGreaterThan(0);
    }

    @Test
    public void getNewHoaxesOfUser_whenUserDoesNotExists_receiveNotFound() {
        var response = getNewHoaxesOfUser(5, "user1", new ParameterizedTypeReference<>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void getNewHoaxesOfUser_whenUserExistsWithNoHoaxes_receivePageWithZeroItemAfterProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createdValidHoax()));
        var hoax = hoaxService.save(user, createdValidHoax());

        var userWithoutHoaxes = userService.save(createValidUser("user2"));

        var response = getNewHoaxesOfUser(hoax.getId(), userWithoutHoaxes.getUsername(), new ParameterizedTypeReference<List<HoaxVM>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).size()).isEqualTo(0);
    }

    @Test
    public void getNewHoaxCount_whenThereAreHoaxes_receiveCountAfterProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createdValidHoax()));
        var fourthHoax = hoaxService.save(user, createdValidHoax());
        hoaxService.save(user, createdValidHoax());

        var response = getNewHoaxCount(fourthHoax.getId(), new ParameterizedTypeReference<Map<String, Long>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).get("count")).isEqualTo(1);
    }

    @Test
    public void getNewHoaxCountOfUser_whenUserExistsWithHoaxes_receiveCountAfterProvidedId() {
        var user = userService.save(createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> hoaxService.save(user, createdValidHoax()));

        var fourthHoax = hoaxService.save(user, createdValidHoax());
        hoaxService.save(user, createdValidHoax());

        var response = getNewHoaxCountOfUser(fourthHoax.getId(), user.getUsername(), new ParameterizedTypeReference<Map<String, Long>>() {
        });
        assertThat(Objects.requireNonNull(response.getBody()).get("count")).isEqualTo(1);
    }

    @Test
    public void deleteHoax_whenUserIsUnauthorized_receiveUnauthorized() {
        var response = deleteHoax(5, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void deleteHoax_whenUserAuthorized_receiveOk() {
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());

        var hoax = createdValidHoax();
        hoaxService.save(user, hoax);

        var response = deleteHoax(hoax.getId(), Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void deleteHoax_whenUserAuthorized_receiveGenericResponse() {
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());

        var hoax = createdValidHoax();

        var response = deleteHoax(hoax.getId(), GenericResponse.class);
        assertThat(Objects.requireNonNull(response.getBody()).getMessage()).isNotNull();
    }

    @Test
    public void deleteHoax_whenUserAuthorized_hoaxRemovedFromDatabase() {
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());

        var hoax = createdValidHoax();
        hoaxService.save(user, hoax);

        deleteHoax(hoax.getId(), GenericResponse.class);
        var inDb = hoaxRepository.findById(hoax.getId());
        assertThat(inDb.isPresent()).isFalse();
    }

    @Test
    public void deleteHoax_whenHoaxIsOwnedByAnotherUser_receiveForbidden() {
        var otherUser = userService.save(createValidUser("user1"));
        authenticate(otherUser.getUsername());

        var owner = userService.save(createValidUser("user2"));
        var hoax = createdValidHoax();
        hoaxService.save(owner, hoax);

        var response = deleteHoax(hoax.getId(), GenericResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void deleteHoax_whenHoaxDoesNotExists_receiveNotFound() {
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());

        var response = deleteHoax(5, GenericResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void deleteHoax_whenHoaxHasAttachment_attachmentRemovedFromDatabase() throws IOException {
        var user = userService.save(createValidUser("user1"));

        MultipartFile file = createFile();
        var savedFile = fileService.saveAttachment(file);

        Hoax hoax = createdValidHoax();
        hoax.setAttachment(savedFile);

        var response = postHoax(hoax, user.getUsername())
                .expectBody(HoaxVM.class)
                .returnResult()
                .getResponseBody();

        var hoaxId = Objects.requireNonNull(response).getId();

        authenticate(user.getUsername());
        deleteHoax(hoaxId, Object.class);
        var optionalAttachment = fileAttachmentRepository.findById(savedFile.getId());

        assertThat(optionalAttachment.isPresent()).isFalse();
    }

    @Test
    public void deleteHoax_whenHoaxHasAttachment_attachmentRemovedFromFileStorage() throws IOException {
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());

        MultipartFile file = createFile();
        var savedFile = fileService.saveAttachment(file);

        Hoax hoax = createdValidHoax();
        hoax.setAttachment(savedFile);
        var response = postHoax(hoax, user.getUsername())
                .expectBody(HoaxVM.class)
                .returnResult()
                .getResponseBody();

        var hoaxId = Objects.requireNonNull(response).getId();
        deleteHoax(hoaxId, Object.class);

        var fileOnDisk = new File(appConfiguration.getFullAttachmentsPath() + "/" + savedFile.getName());
        assertThat(fileOnDisk.exists()).isFalse();
    }

    private <T> ResponseEntity<T> deleteHoax(long hoaxId, Class<T> responseType) {
        return testRestTemplate.exchange(API_1_0_HOAXES + "/" + hoaxId, HttpMethod.DELETE, null, responseType);
    }

    private <T> ResponseEntity<T> getNewHoaxCountOfUser(long hoaxId, String username, ParameterizedTypeReference<T> responseType) {
        var path = "/api/1.0/users/" + username + "/hoaxes/" + hoaxId + "?direction=after&count=true";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getNewHoaxCount(long hoaxId, ParameterizedTypeReference<T> responseType) {
        var path = API_1_0_HOAXES + "/" + hoaxId + "?direction=after&count=true";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getNewHoaxesOfUser(long hoaxId, String username, ParameterizedTypeReference<T> responseType) {
        var path = "/api/1.0/users/" + username + "/hoaxes/" + hoaxId + "?direction=after&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getNewHoaxes(long hoaxId, ParameterizedTypeReference<T> responseType) {
        var path = API_1_0_HOAXES + "/" + hoaxId + "?direction=after&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getOldHoaxesOfUser(long hoaxId, String username, ParameterizedTypeReference<T> responseType) {
        var path = "/api/1.0/users/" + username + "/hoaxes/" + hoaxId + "?direction=before&page=0&size=5&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getOldHoaxes(long hoaxId, ParameterizedTypeReference<T> responseType) {
        var path = API_1_0_HOAXES + "/" + hoaxId + "?direction=before&page=0&size=5&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getHoaxesOfUser(String username, ParameterizedTypeReference<T> responseType) {
        return testRestTemplate.exchange("/api/1.0/users/" + username + "/hoaxes", HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getHoaxesOfUser(ParameterizedTypeReference<T> responseType) {
        return testRestTemplate.exchange(API_1_0_HOAXES, HttpMethod.GET, null, responseType);
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

    private void authenticate2(String username) {
        webTestClient.options().headers(httpHeaders -> httpHeaders.setBasicAuth(username, TEST_PASSWORD));
    }

    private void authenticate(String username) {
        testRestTemplate.getRestTemplate().getInterceptors().add(new BasicAuthenticationInterceptor(username, "P4ssword"));
    }
}