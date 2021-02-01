package no.javatec.hoaxify.file;

import lombok.RequiredArgsConstructor;
import no.javatec.hoaxify.configuration.AppConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@EnableScheduling
public class FileService {

    private static final String TRANSPORT_LOCATIONS = "transportLocations";
    private static final String CACHE_MANAGER = "cacheManager";
    private static final String ROOT_METHOD_NAME = "#root.methodName";

    private final AppConfiguration appConfiguration;
    private final FileAttachmentRepository fileAttachmentRepository;

    private final Tika tika = new Tika();

    @Cacheable(value = TRANSPORT_LOCATIONS, cacheManager = CACHE_MANAGER, key = ROOT_METHOD_NAME, sync = true)
    public String saveProfileImage(String base64Image) throws IOException {
        String imageName = getRandomName();
        var decodedBytes = Base64.getDecoder().decode(base64Image);

        var target = new File(appConfiguration.getFullProfileImagesPath() + "/" + imageName);
        FileUtils.writeByteArrayToFile(target, decodedBytes);

        return imageName;
    }

    public String detectType(byte[] fileArr) {
        return this.tika.detect(fileArr);
    }

    public void deleteProfileImage(String imageName) {
        try {
            Files.deleteIfExists(Path.of(appConfiguration.getFullProfileImagesPath() + "/" + imageName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileAttachment saveAttachment(MultipartFile file) {
        FileAttachment fileAttachment = new FileAttachment();
        fileAttachment.setDate(new Date());

        String randomName = getRandomName();
        fileAttachment.setName(randomName);

        var target = new File(appConfiguration.getFullAttachmentsPath() + "/" + randomName);
        try {
            var fileAsBytes = file.getBytes();
            fileAttachment.setFileType(detectType(fileAsBytes));
            FileUtils.writeByteArrayToFile(target, fileAsBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileAttachmentRepository.save(fileAttachment);
    }

    //@Scheduled(fixedRate = 60 * 60 * 1000)
    public void cleanupStorage() {
        Date oneHourAgo = new Date(System.currentTimeMillis() - 60 * 60 * 1000);
        var oldFiles = fileAttachmentRepository.findByDateBeforeAndHoaxIsNull(oneHourAgo);

        for (var attachment : oldFiles) {
            deleteAttachmentImage(attachment.getName());
            fileAttachmentRepository.deleteById(attachment.getId());
        }
    }

    public void deleteAttachmentImage(String image) {
        try {
            Files.deleteIfExists(Path.of(appConfiguration.getFullAttachmentsPath() + "/" + image));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getRandomName() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
