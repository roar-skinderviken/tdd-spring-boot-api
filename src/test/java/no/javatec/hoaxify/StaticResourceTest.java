package no.javatec.hoaxify;

import no.javatec.hoaxify.configuration.AppConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class StaticResourceTest {

    @Autowired
    AppConfiguration appConfiguration;

    @Autowired
    MockMvc mockMvc;

    @Test
    public void checkStaticFolder_whenAppIsInitialized_uploadFolderMustExists() {
        File uploadFolder = new File(appConfiguration.getUploadPath());
        assertThat(uploadFolder.exists() && uploadFolder.isDirectory()).isTrue();
    }

    @Test
    public void checkStaticFolder_whenAppIsInitialized_profileImageSubFolderMustExists() {
        File folder = new File(appConfiguration.getFullProfileImagesPath());
        assertThat(folder.exists() && folder.isDirectory()).isTrue();
    }

    @Test
    public void checkStaticFolder_whenAppIsInitialized_attachmentsSubFolderMustExists() {
        File folder = new File(appConfiguration.getFullAttachmentsPath());
        assertThat(folder.exists() && folder.isDirectory()).isTrue();
    }

    @Test
    public void getStaticFile_whenImageExistsInProfileUploadFolder_receiveOk() throws Exception {
        var source = new ClassPathResource("profile.png").getFile();

        var fileName = "profile-picture.png";
        var target = new File(appConfiguration.getFullProfileImagesPath() + "/" + fileName);
        FileUtils.copyFile(source, target);

        mockMvc.perform(
                get("/images/" + appConfiguration.getProfileImagesFolder() + "/" + fileName))
                .andExpect(status().isOk());
    }

    @Test
    public void getStaticFile_whenImageExistsInAttachmentsFolder_receiveOk() throws Exception {
        var source = new ClassPathResource("profile.png").getFile();

        var fileName = "profile-picture.png";
        var target = new File(appConfiguration.getFullAttachmentsPath() + "/" + fileName);
        FileUtils.copyFile(source, target);

        mockMvc.perform(
                get("/images/" + appConfiguration.getAttachmentsFolder() + "/" + fileName))
                .andExpect(status().isOk());
    }

    @Test
    public void getStaticFile_whenImageDoesNotExists_receiveNotFound() throws Exception {
        mockMvc.perform(
                get("/images/" + appConfiguration.getAttachmentsFolder() + "/non-existing.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getStaticFile_whenImageExistsInAttachmentsFolder_receiveOkWithCacheHeaders() throws Exception {
        var source = new ClassPathResource("profile.png").getFile();

        var fileName = "profile-picture.png";
        var target = new File(appConfiguration.getFullAttachmentsPath() + "/" + fileName);
        FileUtils.copyFile(source, target);

        var result = mockMvc.perform(
                get("/images/" + appConfiguration.getAttachmentsFolder() + "/" + fileName))
                .andReturn();

        var cacheControl = result.getResponse().getHeaderValue("Cache-Control").toString();
        assertThat(cacheControl.contains("max-age=31536000")).isTrue();
    }

    @AfterEach
    public void cleanup() throws IOException {
        FileUtils.cleanDirectory(new File(appConfiguration.getFullProfileImagesPath()));
        FileUtils.cleanDirectory(new File(appConfiguration.getFullAttachmentsPath()));
    }
}
