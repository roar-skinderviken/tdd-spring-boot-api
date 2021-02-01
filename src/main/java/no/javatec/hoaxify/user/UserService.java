package no.javatec.hoaxify.user;

import lombok.RequiredArgsConstructor;
import no.javatec.hoaxify.error.NotFoundException;
import no.javatec.hoaxify.file.FileService;
import no.javatec.hoaxify.user.vm.UserUpdateVM;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileService fileService;

    public User save(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Page<User> getUsers(User loggedInUser, Pageable pageable) {
        if (loggedInUser == null){
            return userRepository.findAll(pageable);
        }
        return userRepository.findByUsernameNot(loggedInUser.getUsername(), pageable);
    }

    public User getByUsername(String username) {
        var user = userRepository.findByUsername(username);
        if (user == null){
            throw new NotFoundException(username + " not found");
        }
        return user;
    }

    public User update(long id, UserUpdateVM userUpdate) {
        var inDb = userRepository.getOne(id);
        inDb.setDisplayName(userUpdate.getDisplayName());

        if (userUpdate.getImage() != null) {
            try {
                var savedImageName = fileService.saveProfileImage(userUpdate.getImage());
                fileService.deleteProfileImage(inDb.getImage());
                inDb.setImage(savedImageName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return userRepository.save(inDb);
    }
}
