package no.javatec.hoaxify.user;

import lombok.RequiredArgsConstructor;
import no.javatec.hoaxify.shared.GenericResponse;
import no.javatec.hoaxify.user.vm.UserUpdateVM;
import no.javatec.hoaxify.user.vm.UserVM;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/1.0")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/users")
    GenericResponse createUser(@Valid @RequestBody User user) {
        userService.save(user);
        return new GenericResponse("User saved");
    }

    // can be using @PageableDefault(size = 10) instead of global
    @GetMapping("/users")
    Page<UserVM> getUsers(@AuthenticationPrincipal User user, Pageable pageable) {
        return userService.getUsers(user, pageable).map(UserVM::new);
    }

    @GetMapping("/users/{username}")
    UserVM getUserByName(@PathVariable String username) {
        var user = userService.getByUsername(username);
        return new UserVM(user);
    }

    @PutMapping("/users/{id:[0-9]+}")
    @PreAuthorize("#id == principal.id")
    UserVM updateUser(@PathVariable long id, @Valid @RequestBody(required = false) UserUpdateVM userUpdate) {
        return new UserVM(userService.update(id, userUpdate));
    }
}
