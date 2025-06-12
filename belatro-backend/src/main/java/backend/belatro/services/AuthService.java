package backend.belatro.services;

import backend.belatro.dtos.JwtResponseDTO;
import backend.belatro.dtos.LoginRequestDTO;
import backend.belatro.dtos.SignupRequestDTO;
import backend.belatro.dtos.UserLoginDetailsDTO;
import backend.belatro.models.User;
import backend.belatro.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Value("${jwt.expiration}")
    private long jwtExpirationMinutes;

    @Autowired
    public AuthService(AuthenticationManager authManager, JwtTokenProvider tokenProvider, PasswordEncoder passwordEncoder, UserService userService) {
        this.authManager = authManager;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }

    public JwtResponseDTO login(LoginRequestDTO loginRequest) {
        String username = normalizeUsername(loginRequest.getUsername());

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                       username,
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(auth);


        String jwt      = tokenProvider.createToken(username, jwtExpirationMinutes);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "User not found after authentication: " + username
                ));

        UserLoginDetailsDTO details =
                new UserLoginDetailsDTO(user.getId(), user.getUsername());

        return new JwtResponseDTO(jwt, details);
    }

    public JwtResponseDTO signup(SignupRequestDTO dto) {
        String username = normalizeUsername(dto.getUsername());

        if (userService.findByUsername(dto.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Error: Username is already taken!");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(dto.getEmail());
        newUser.setEloRating(1200);
        newUser.setPasswordHashed(passwordEncoder.encode(dto.getPassword()));

        User createdUser = userService.createUser(newUser);
        if (createdUser == null || createdUser.getId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error: Could not complete user registration.");
        }

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.getUsername(),
                        dto.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        String jwt = tokenProvider.createToken(auth.getName(), jwtExpirationMinutes);

        UserLoginDetailsDTO details =
                new UserLoginDetailsDTO(createdUser.getId(), createdUser.getUsername());

        return new JwtResponseDTO(jwt, details);
    }

    public void logout(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7);
            tokenProvider.invalidateToken(token);
        }
        SecurityContextHolder.clearContext();
    }
    private static String normalizeUsername(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }

}