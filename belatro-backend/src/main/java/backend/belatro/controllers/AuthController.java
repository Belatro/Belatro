package backend.belatro.controllers;

import backend.belatro.dtos.JwtResponseDTO;
import backend.belatro.dtos.LoginRequestDTO;
import backend.belatro.dtos.SignupRequestDTO;
import backend.belatro.dtos.UserLoginDetailsDTO;
import backend.belatro.models.User;
import backend.belatro.security.JwtTokenProvider;
import backend.belatro.services.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @Value("${jwt.expiration}")
    private long jwtExpirationMinutes;

    public AuthController(
   AuthenticationManager authManager,
   JwtTokenProvider tokenProvider,
   PasswordEncoder passwordEncoder, UserService userService
  ) {
        this.authManager     = authManager;
        this.tokenProvider   = tokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.userService     = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponseDTO> login(@RequestBody LoginRequestDTO loginRequest) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Correctly get username from the Authentication object
        String username = auth.getName();
        String jwt = tokenProvider.createToken(username, jwtExpirationMinutes);

        // Fetch the User entity to get ID
        User userEntity = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found after authentication: " + username));

        // Create DTO with user details
        UserLoginDetailsDTO userDetails = new UserLoginDetailsDTO(userEntity.getId(), userEntity.getUsername());

        return ResponseEntity.ok(new JwtResponseDTO(jwt, userDetails));
    }



    @PostMapping("/signup")
    public ResponseEntity<JwtResponseDTO> signup(@RequestBody SignupRequestDTO dto) {
        if (userService.findByUsername(dto.getUsername()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new JwtResponseDTO("Error: Username is already taken!")); // Uses message constructor
        }

        User newUser = new User();
        newUser.setUsername(dto.getUsername());
        newUser.setEmail(dto.getEmail());
        newUser.setEloRating(1200); // Default ELO
        newUser.setPasswordHashed(passwordEncoder.encode(dto.getPassword()));

        // Capture the created user to get its ID
        User createdUser = userService.createUser(newUser);
        if (createdUser == null || createdUser.getId() == null) {
            return ResponseEntity.internalServerError().body(new JwtResponseDTO("Error: Could not complete user registration."));
        }

        // Authenticate the new user
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.getUsername(), // Use username from DTO
                        dto.getPassword()  // Use plain password from DTO for this authentication step
                )
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        String jwt = tokenProvider.createToken(auth.getName(), jwtExpirationMinutes);

        // Create DTO with user details from the createdUser
        UserLoginDetailsDTO userDetails = new UserLoginDetailsDTO(createdUser.getId(), createdUser.getUsername());

        // Return JWT and user details
        return ResponseEntity.ok(new JwtResponseDTO(jwt, userDetails));
    }

}
