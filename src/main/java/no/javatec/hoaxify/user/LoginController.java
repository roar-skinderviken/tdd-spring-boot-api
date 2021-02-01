package no.javatec.hoaxify.user;

import no.javatec.hoaxify.user.vm.UserVM;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {
    @PostMapping("/api/1.0/login")
    UserVM handleLogin(@AuthenticationPrincipal User user) {
        return new UserVM(user);
    }
}
