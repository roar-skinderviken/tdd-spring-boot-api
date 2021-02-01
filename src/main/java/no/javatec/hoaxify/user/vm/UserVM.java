package no.javatec.hoaxify.user.vm;

import lombok.Data;
import lombok.NoArgsConstructor;
import no.javatec.hoaxify.user.User;

@Data
@NoArgsConstructor
public class UserVM {
    private long id;
    private String username;
    private String displayName;
    private String image;

    public UserVM(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.displayName = user.getDisplayName();
        this.image = user.getImage();
    }
}
