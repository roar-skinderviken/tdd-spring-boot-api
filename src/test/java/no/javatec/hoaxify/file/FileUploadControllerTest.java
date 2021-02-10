package no.javatec.hoaxify.file;

import no.javatec.hoaxify.configuration.AppConfiguration;
import no.javatec.hoaxify.user.UserRepository;
import no.javatec.hoaxify.user.UserService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static no.javatec.hoaxify.TestUtils.TEST_PASSWORD;
import static no.javatec.hoaxify.TestUtils.createValidUser;
import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class FileUploadControllerTest {

    private static final String API_1_0_HOAXES_UPLOAD = "/api/1.0/hoaxes/upload";

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    UserRepository userRepository;

    @Autowired
    FileAttachmentRepository fileAttachmentRepository;

    @Autowired
    UserService userService;

    @Autowired
    FileService fileService;

    @Autowired
    AppConfiguration appConfiguration;

    @BeforeEach
    public void init() throws IOException {
        userRepository.deleteAll();
        fileAttachmentRepository.deleteAll();
        FileUtils.cleanDirectory(new File(appConfiguration.getFullAttachmentsPath()));
    }

    @Test
    public void uploadFile_withImageFromAuthUser_receiveOk() {
        var user = userService.save(createValidUser("user1"));

        uploadFile2(user.getUsername())
                .expectStatus().isOk();
    }

    @Test
    public void uploadFile_withUnauthorizedUser_receiveUnauthorized() {
        uploadFile2(null)
                .expectStatus().isUnauthorized();
    }

    @Test
    public void uploadFile_withImageFromAuthUser_receiveFileAttachmentWithDate() {
        var user = userService.save(createValidUser("user1"));

        uploadFile2(user.getUsername())
                .expectBody(FileAttachment.class)
                .value(attachment -> assertThat(attachment.getDate()).isNotNull());
    }

    @Test
    public void uploadFile_withImageFromAuthUser_receiveFileAttachmentWithRandomName() {
        var user = userService.save(createValidUser("user1"));

        uploadFile2(user.getUsername())
                .expectBody(FileAttachment.class)
                .value(attachment -> {
                    assertThat(attachment.getName()).isNotNull();
                    assertThat(attachment.getName()).isNotEqualTo("profile.png");
                });
    }

    @Test
    public void uploadFile_withImageFromAuthUser_imageSavedToFolder() {
        var user = userService.save(createValidUser("user1"));

        var fileAttachment = uploadFile2(user.getUsername())
                .expectBody(FileAttachment.class)
                .returnResult()
                .getResponseBody();

        var file = new File(appConfiguration.getFullAttachmentsPath() + "/" + Objects.requireNonNull(fileAttachment).getName());
        assertThat(file.exists()).isTrue();
    }

    @Test
    public void uploadFile_withImageFromAuthUser_imageSavedToDatabase() {
        var user = userService.save(createValidUser("user1"));
        uploadFile2(user.getUsername());
        assertThat(fileAttachmentRepository.count()).isEqualTo(1);
    }

    @Test
    public void uploadFile_withImageFromAuthUser_fileAttachmentStoredWithFileType() {
        var user = userService.save(createValidUser("user1"));
        uploadFile2(user.getUsername());
        assertThat(fileAttachmentRepository.findAll().get(0).getFileType()).isEqualTo("image/png");
    }

    private WebTestClient.ResponseSpec uploadFile2(String loggedInUsername) {
        var clientBuilder = webTestClient.post()
                .uri(API_1_0_HOAXES_UPLOAD);

        if (loggedInUsername != null) {
            clientBuilder.headers(httpHeaders -> httpHeaders.setBasicAuth(loggedInUsername, TEST_PASSWORD));
        }

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        multipartBodyBuilder
                .part("file", new ClassPathResource("profile.png"))
                .contentType(MediaType.MULTIPART_FORM_DATA);

        return clientBuilder
                .body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
                .exchange();
    }
}