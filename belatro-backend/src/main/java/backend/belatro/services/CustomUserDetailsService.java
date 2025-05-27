package backend.belatro.services;

import backend.belatro.enums.Role;
import backend.belatro.models.User;
import backend.belatro.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static backend.belatro.configs.SecurityConfig.log;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final SimpleGrantedAuthority DEFAULT_AUTHORITY =
            new SimpleGrantedAuthority("ROLE_USER");

    private final UserRepo userRepo;

    @Autowired
    public CustomUserDetailsService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }
    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepo.findByUsername(username.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("No user found: " + username));
        log.debug("Authenticating '{}'", username);

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHashed())
                .authorities(mapRolesToAuthorities(user.getRoles()))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    private Collection<SimpleGrantedAuthority> mapRolesToAuthorities(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of(DEFAULT_AUTHORITY);
        }
        return roles.stream()
                .map(Enum::name)               // Role enum → "ROLE_X"
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}