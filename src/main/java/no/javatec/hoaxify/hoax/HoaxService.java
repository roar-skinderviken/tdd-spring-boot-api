package no.javatec.hoaxify.hoax;

import lombok.RequiredArgsConstructor;
import no.javatec.hoaxify.file.FileAttachmentRepository;
import no.javatec.hoaxify.file.FileService;
import no.javatec.hoaxify.user.User;
import no.javatec.hoaxify.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HoaxService {

    private final UserService userService;
    private final HoaxRepository hoaxRepository;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final FileService fileService;

    @Transactional
    public Hoax save(User user, Hoax hoax) {
        hoax.setTimestamp(new Date());
        hoax.setUser(user);

        if (hoax.getAttachment() != null) {
            var inDb = fileAttachmentRepository.findById(hoax.getAttachment().getId()).get();
            inDb.setHoax(hoax);
            hoax.setAttachment(inDb);
        }

        return this.hoaxRepository.save(hoax);
    }

    public Page<Hoax> getAllHoaxes(Pageable pageable) {
        return this.hoaxRepository.findAll(pageable);
    }

    public Page<Hoax> getHoaxesOfUser(String username, Pageable pageable) {
        var user = userService.getByUsername(username);
        return this.hoaxRepository.findByUser(user, pageable);
    }

    public Page<Hoax> getOldHoaxes(long id, String username, Pageable pageable) {
        Specification<Hoax> spec = Specification.where(idLessThan(id));
        if (username != null) {
            spec = spec.and(userIs(userService.getByUsername(username)));
        }
        return this.hoaxRepository.findAll(spec, pageable);
    }

    public List<Hoax> getNewHoaxes(long id, String username, Pageable pageable) {
        Specification<Hoax> spec = Specification.where(idGreaterThan(id));
        if (username != null) {
            spec = spec.and(userIs(userService.getByUsername(username)));
        }
        return this.hoaxRepository.findAll(spec, pageable.getSort());
    }

    public long getNewHoaxCount(long id, String username) {
        Specification<Hoax> spec = Specification.where(idGreaterThan(id));
        if (username != null) {
            spec = spec.and(userIs(userService.getByUsername(username)));
        }
        return hoaxRepository.count(spec);
    }

    @Transactional
    public void deleteHoax(long hoaxId) {
        var hoax = hoaxRepository.getOne(hoaxId);
        if (hoax.getAttachment() != null) {
            fileService.deleteAttachmentImage(hoax.getAttachment().getName());
        }
        hoaxRepository.deleteById(hoaxId);
    }

    private Specification<Hoax> userIs(User user) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.equal(root.get("user"), user);
    }

    private Specification<Hoax> idLessThan(long id) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.lessThan(root.get("id"), id);
    }

    private Specification<Hoax> idGreaterThan(long id) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.greaterThan(root.get("id"), id);
    }
}
