package no.javatec.hoaxify.hoax.vm;

import lombok.Data;
import lombok.NoArgsConstructor;
import no.javatec.hoaxify.file.FileAttachmentVM;
import no.javatec.hoaxify.hoax.Hoax;
import no.javatec.hoaxify.user.vm.UserVM;

@Data
@NoArgsConstructor
public class HoaxVM {

    private long id;
    private String content;
    private long date;
    private UserVM user;
    private FileAttachmentVM attachment;

    public HoaxVM(Hoax hoax) {
        this.id = hoax.getId();
        this.content = hoax.getContent();
        this.date = hoax.getTimestamp().getTime();
        this.user = new UserVM(hoax.getUser());
        if (hoax.getAttachment() != null) {
            this.attachment = new FileAttachmentVM(hoax.getAttachment());
        }
    }
}
