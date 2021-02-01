package no.javatec.hoaxify.user;

import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

    public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String> {

        @Autowired
        UserRepository userRepository;

        @Override
        public boolean isValid(String username, ConstraintValidatorContext constraintValidatorContext) {
            return userRepository.findByUsername(username) == null;
        }
    }
