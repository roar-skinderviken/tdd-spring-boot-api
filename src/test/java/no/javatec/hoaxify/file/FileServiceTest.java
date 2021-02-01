package no.javatec.hoaxify.file;

import no.javatec.hoaxify.configuration.AppConfiguration;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class FileServiceTest {

    FileService fileService;

    AppConfiguration appConfiguration;

    @MockBean
    FileAttachmentRepository fileAttachmentRepository;

    @BeforeEach
    public void init() {
        appConfiguration = new AppConfiguration();
        appConfiguration.setUploadPath("uploads-test");

        fileService = new FileService(appConfiguration, fileAttachmentRepository);

        new File(appConfiguration.getUploadPath()).mkdir();
        new File(appConfiguration.getFullProfileImagesPath()).mkdir();
        new File(appConfiguration.getFullAttachmentsPath()).mkdir();
    }

    @Test
    public void detectType_whenPngFileProvided_returnsImagePng() throws IOException {
        ClassPathResource resource = new ClassPathResource("test-png.png");
        var fileArr = FileUtils.readFileToByteArray(resource.getFile());
        var fileType = fileService.detectType(fileArr);
        assertThat(fileType).isEqualTo("image/png");
    }

    @Test
    public void cleanupStorage_whenOldFilesExists_removeFilesFromStorage() throws IOException {
        File source = new ClassPathResource("profile.png").getFile();

        var fileName = "random-file";
        var filePath = appConfiguration.getFullAttachmentsPath() + "/" + fileName;
        var target = new File(filePath);

        FileUtils.copyFile(source, target);

        FileAttachment fileAttachment = new FileAttachment();
        fileAttachment.setId(5);
        fileAttachment.setName(fileName);

        Mockito.when(fileAttachmentRepository.findByDateBeforeAndHoaxIsNull(Mockito.any(Date.class)))
                .thenReturn(List.of(fileAttachment));

        fileService.cleanupStorage();
        File storedImage = new File(filePath);

        assertThat(storedImage.exists()).isFalse();
    }

    @Test
    public void cleanupStorage_whenOldFilesExists_removeFilesFromDatabase() throws IOException {
        File source = new ClassPathResource("profile.png").getFile();

        var fileName = "random-file";
        var filePath = appConfiguration.getFullAttachmentsPath() + "/" + fileName;
        var target = new File(filePath);

        FileUtils.copyFile(source, target);

        FileAttachment fileAttachment = new FileAttachment();
        fileAttachment.setId(5);
        fileAttachment.setName(fileName);

        Mockito.when(fileAttachmentRepository.findByDateBeforeAndHoaxIsNull(Mockito.any(Date.class)))
                .thenReturn(List.of(fileAttachment));

        fileService.cleanupStorage();

        Mockito.verify(fileAttachmentRepository).deleteById(5L);
    }

    @AfterEach
    public void cleanup() throws IOException {
        FileUtils.cleanDirectory(new File(appConfiguration.getFullProfileImagesPath()));
        FileUtils.cleanDirectory(new File(appConfiguration.getFullAttachmentsPath()));
    }
}