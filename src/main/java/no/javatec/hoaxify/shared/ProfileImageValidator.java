package no.javatec.hoaxify.shared;

import no.javatec.hoaxify.file.FileService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Base64;
import java.util.List;

public class ProfileImageValidator implements ConstraintValidator<ProfileImage, String> {

    private static final List<String> ALLOWED_FILETYPES = List.of("image/png", "image/jpeg");

    @Autowired
    FileService fileService;

    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null){
            return true;
        }
        var decodedBytes = Base64.getDecoder().decode(value);
        var fileType = fileService.detectType(decodedBytes);
        return ALLOWED_FILETYPES.contains(fileType);
    }
}
