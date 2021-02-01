package no.javatec.hoaxify.file;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FileAttachmentVM {

    private String name;
    private String fileType;

    public FileAttachmentVM(FileAttachment fileAttachment) {
        this.name = fileAttachment.getName();
        this.fileType = fileAttachment.getFileType();
    }
}
