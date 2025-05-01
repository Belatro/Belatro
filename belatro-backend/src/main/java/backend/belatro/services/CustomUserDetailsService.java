package backend.belatro.services;

import backend.belatro.models.User;
import backend.belatro.repos.UserRepo;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;

    public CustomUserDetailsService(UserRepo userRepository) {
        this.userRepo = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("No user found: " + username)
                );
        // Adapt your User entity to Spring’s UserDetails:
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHashed())
                .roles("USER")    // or whatever roles you need
                .build();
    }
}

