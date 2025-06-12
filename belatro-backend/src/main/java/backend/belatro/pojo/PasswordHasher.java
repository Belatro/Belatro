package backend.belatro.pojo;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHasher {
    public static void main(String[] args) {
        String raw = "123";                // ← your chosen password
        String hashed = new BCryptPasswordEncoder().encode(raw);
        System.out.println(hashed);
    }
}