package backend.belatro.services;


import backend.belatro.dtos.UserUpdateDTO;
import backend.belatro.exceptions.UserNotFoundException;
import backend.belatro.models.User;
import backend.belatro.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepo userRepo;
    @Autowired
    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public List<User> findAll() {
        return userRepo.findAll();
    }

    public Optional<User> findById(String id) {
        return userRepo.findById(id);
    }

    public User createUser(User user) {
        user.setId(null);
        user.setEloRating(1200);
        return userRepo.save(user);
    }
    @Transactional
    public User updateUser(String id, UserUpdateDTO updateDTO) {
        User existingUser = userRepo.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + id));

        existingUser.setUsername(updateDTO.getUsername());
        existingUser.setEmail(updateDTO.getEmail());
        existingUser.setPasswordHashed(updateDTO.getPasswordHashed());
        existingUser.setEloRating(updateDTO.getEloRating());
        existingUser.setLevel(updateDTO.getLevel());
        existingUser.setExpPoints(updateDTO.getExpPoints());
        existingUser.setLastLogin(updateDTO.getLastLogin());
        existingUser.setGamesPlayed(updateDTO.getGamesPlayed());

        return userRepo.save(existingUser);
    }
    public void deleteUser(String id) {
        userRepo.deleteById(id);
    }

    public Optional<User> findByUsername(String rawUsername) {
        return userRepo.findByUsername(
                rawUsername == null ? null : rawUsername.trim().toLowerCase(Locale.ROOT)
        );
    }
    public User currentUser() throws AccessDeniedException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Not authenticated");
        }

        // ‘getName()’ is the username by default
        return userRepo.findByUsername(auth.getName())
                .orElseThrow(() ->
                        new UsernameNotFoundException(auth.getName()));
    }
}


