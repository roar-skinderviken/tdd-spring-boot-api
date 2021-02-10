package no.javatec.hoaxify.file;

import no.javatec.hoaxify.TestUtils;
import no.javatec.hoaxify.hoax.Hoax;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class FileAttachmentRepositoryTest {

    @Autowired
    TestEntityManager testEntityManager;

    @Autowired
    FileAttachmentRepository fileAttachmentRepository;

    @Test
    public void findByDateBeforeAndHoaxIsNull_whenAttachmentsOlderThanOneHour_returnAll() {
        testEntityManager.persist(getOneHourOldFileAttachment());
        testEntityManager.persist(getOneHourOldFileAttachment());
        testEntityManager.persist(getOneHourOldFileAttachment());

        var result = fileAttachmentRepository.findByDateBeforeAndHoaxIsNull(new Date(oneHourAgoInMilliSecs()));
        assertThat(result.size()).isEqualTo(3);
    }

    @Test
    public void findByDateBeforeAndHoaxIsNull_whenAttachmentsOlderThanOneHourButHaveHoax_returnNone() {
        var hoax1 = testEntityManager.persist(TestUtils.createValidHoax());
        var hoax2 = testEntityManager.persist(TestUtils.createValidHoax());
        var hoax3 = testEntityManager.persist(TestUtils.createValidHoax());

        testEntityManager.persist(getOldFileAttachmentWithHoax(hoax1));
        testEntityManager.persist(getOldFileAttachmentWithHoax(hoax2));
        testEntityManager.persist(getOldFileAttachmentWithHoax(hoax3));

        var result = fileAttachmentRepository.findByDateBeforeAndHoaxIsNull(new Date(oneHourAgoInMilliSecs()));
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void findByDateBeforeAndHoaxIsNull_whenAttachmentsWithinOneHour_returnNone() {
        testEntityManager.persist(getFileAttachmentWithinOneHour());
        testEntityManager.persist(getFileAttachmentWithinOneHour());
        testEntityManager.persist(getFileAttachmentWithinOneHour());

        var result = fileAttachmentRepository.findByDateBeforeAndHoaxIsNull(new Date(oneHourAgoInMilliSecs()));
        assertThat(result.size()).isEqualTo(0);
    }

    private static long oneHourAgoInMilliSecs() {
        return System.currentTimeMillis() - 60 * 60 * 1000;
    }

    private FileAttachment getOldFileAttachmentWithHoax(Hoax hoax) {
        var attachment = getOneHourOldFileAttachment();
        attachment.setHoax(hoax);
        return attachment;
    }

    private FileAttachment getFileAttachmentWithinOneHour() {
        var fileAttachment = new FileAttachment();
        fileAttachment.setFileType("image/png");
        fileAttachment.setName("profile.png");
        fileAttachment.setDate(new Date(oneHourAgoInMilliSecs() + 1000));
        return fileAttachment;
    }

    private FileAttachment getOneHourOldFileAttachment() {
        var fileAttachment = new FileAttachment();
        fileAttachment.setFileType("image/png");
        fileAttachment.setName("profile.png");
        fileAttachment.setDate(new Date(oneHourAgoInMilliSecs() - 1));
        return fileAttachment;
    }
}