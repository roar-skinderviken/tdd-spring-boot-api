package no.javatec.hoaxify.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.text.MessageFormat;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "hoaxify")
public class AppConfiguration {

    private String uploadPath;
    private String profileImagesFolder = "profile";
    private String attachmentsFolder = "attachments";
    private Map<String, String> labels;

    public String getFullProfileImagesPath() {
        return MessageFormat.format("{0}/{1}", this.uploadPath, this.profileImagesFolder);
    }

    public String getFullAttachmentsPath() {
        return MessageFormat.format("{0}/{1}", this.uploadPath, this.attachmentsFolder);
    }
}
