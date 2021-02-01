package no.javatec.hoaxify.hoax;

import lombok.RequiredArgsConstructor;
import no.javatec.hoaxify.user.User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HoaxSecurityService {

    private final HoaxRepository hoaxRepository;

    public boolean isAllowedToDelete(long hoaxId, User loggedInUser) {
        var hoax = hoaxRepository.findById(hoaxId);
        return hoax.isPresent() && hoax.get().getUser().getId() == loggedInUser.getId();
    }
}
