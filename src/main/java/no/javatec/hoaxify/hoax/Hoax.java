package no.javatec.hoaxify.hoax;

import lombok.Data;
import no.javatec.hoaxify.file.FileAttachment;
import no.javatec.hoaxify.user.User;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

@Data
@Entity
public class Hoax {

    @Id
    @GeneratedValue
    private long id;

    @NotNull
    @Size(min = 10, max = 5000)
    @Column(length = 5000)
    private String content;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @ManyToOne
    private User user;

    @OneToOne(mappedBy = "hoax", orphanRemoval = true)
    private FileAttachment attachment;
}
