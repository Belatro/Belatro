package backend.belatro.controllers;

import backend.belatro.dtos.JwtResponseDTO;
import backend.belatro.dtos.LoginRequestDTO;
import backend.belatro.dtos.SignupRequestDTO;
import backend.belatro.models.User;
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


import backend.belatro.security.JwtTokenProvider;

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
        String jwt = tokenProvider.createToken(auth.getName(), 60);
        return ResponseEntity.ok(new JwtResponseDTO(jwt));
    }


    @PostMapping("/signup")
    public ResponseEntity<JwtResponseDTO> signup(@RequestBody SignupRequestDTO dto) {

        if (userService.findByUsername(dto.getUsername()).isPresent()) {
            return ResponseEntity
                    .badRequest()
                    .body(new JwtResponseDTO("Error: Username is already taken!"));
        }

        User newUser = new User();
        newUser.setUsername(dto.getUsername());
        newUser.setEmail(dto.getEmail());
        newUser.setEloRating(1200);
        newUser.setPasswordHashed(
                passwordEncoder.encode(dto.getPassword())
        );
        userService.createUser(newUser);

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.getUsername(),
                        dto.getPassword()
                )
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
        String jwt = tokenProvider.createToken(auth.getName(), jwtExpirationMinutes);

        return ResponseEntity.ok(new JwtResponseDTO(jwt));
    }
}
