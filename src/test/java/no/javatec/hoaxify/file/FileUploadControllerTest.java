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
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
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
    TestRestTemplate testRestTemplate;

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
        testRestTemplate.getRestTemplate().getInterceptors().clear();
        FileUtils.cleanDirectory(new File(appConfiguration.getFullAttachmentsPath()));
    }

    @Test
    public void uploadFile_withImageFromAuthUser_receiveOk() {

        // create validated user
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());

/*
        webTestClient.post()
                .uri(API_1_0_HOAXES_UPLOAD)
                .exchange()
                .expectStatus().isOk();

        var username =

        webTestClient.post()
                .uri("/user/repos")
                //.header("Authorization", "Basic " + Base64Utils.encodeToString((username + ":P4ssword").getBytes(UTF_8)))
                .body(BodyInserters.fromPublisher(Mono.just("data"), String.class))
                .exchange()
                .expectStatus().isOk();
*/


        ResponseEntity<Object> response = uploadFile(getRequestEntity(), Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void uploadFile_withUnauthorizedUser_receiveUnauthorized() {
        var response = uploadFile(getRequestEntity(), Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void uploadFile_withImageFromAuthUser_receiveFileAttachmentWithDate() {
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());

        ResponseEntity<FileAttachment> response = uploadFile(getRequestEntity(), FileAttachment.class);
        assertThat(response.getBody().getDate()).isNotNull();
    }

    @Test
    public void uploadFile_withImageFromAuthUser_receiveFileAttachmentWithRandomName() {
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());

        ResponseEntity<FileAttachment> response = uploadFile(getRequestEntity(), FileAttachment.class);
        assertThat(response.getBody().getName()).isNotNull();
        assertThat(response.getBody().getName()).isNotEqualTo("profile.png");
    }

    @Test
    public void uploadFile_withImageFromAuthUser_imageSavedToFolder() {
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());

        ResponseEntity<FileAttachment> response = uploadFile(getRequestEntity(), FileAttachment.class);

        var file = new File(appConfiguration.getFullAttachmentsPath() + "/" + response.getBody().getName());
        assertThat(file.exists()).isTrue();
    }

    @Test
    public void uploadFile_withImageFromAuthUser_imageSavedToDatabase() {
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());
        uploadFile(getRequestEntity(), FileAttachment.class);
        assertThat(fileAttachmentRepository.count()).isEqualTo(1);
    }

    @Test
    public void uploadFile_withImageFromAuthUser_fileAttachmentStoredWithFileType() {
        var user = userService.save(createValidUser("user1"));
        authenticate(user.getUsername());
        uploadFile(getRequestEntity(), FileAttachment.class);
        assertThat(fileAttachmentRepository.findAll().get(0).getFileType()).isEqualTo("image/png");
    }

    private <T> ResponseEntity<T> uploadFile(HttpEntity<?> requestEntity, Class<T> responseType) {
        return testRestTemplate.exchange(API_1_0_HOAXES_UPLOAD, HttpMethod.POST, requestEntity, responseType);
    }

    private HttpEntity<MultiValueMap<String, Object>> getRequestEntity() {
        ClassPathResource imageResource = new ClassPathResource("profile.png");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", imageResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return new HttpEntity<>(body, headers);
    }

    private void authenticate(String username) {
        testRestTemplate.getRestTemplate().getInterceptors().add(new BasicAuthenticationInterceptor(username, "P4ssword"));
    }
}