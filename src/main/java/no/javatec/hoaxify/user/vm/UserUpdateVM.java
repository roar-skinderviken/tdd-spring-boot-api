package no.javatec.hoaxify.user.vm;

import lombok.Data;
import lombok.NoArgsConstructor;
import no.javatec.hoaxify.shared.ProfileImage;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
public class UserUpdateVM {

    @NotNull
    @Size(min = 4, max = 255)
    private String displayName;

    @ProfileImage(message = "{hoaxify.constraints.image.ProfileImage.message}")
    private String image;

    public UserUpdateVM(String displayName) {
        this.displayName = displayName;
    }
}
