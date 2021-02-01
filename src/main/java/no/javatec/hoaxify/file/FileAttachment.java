package no.javatec.hoaxify.file;

import lombok.Data;
import no.javatec.hoaxify.hoax.Hoax;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
public class FileAttachment {

    @Id
    @GeneratedValue
    private long id;

    @Temporal(TemporalType.TIMESTAMP)
    private Date date;

    private String name;

    private String fileType;

    @OneToOne
    private Hoax hoax;
}
