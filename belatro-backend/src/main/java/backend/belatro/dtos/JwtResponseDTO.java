package backend.belatro.dtos;

import lombok.Data;

@Data
public class JwtResponseDTO {
    private String token;
    private UserLoginDetailsDTO user; // Added to hold user details
    private String message;


    public JwtResponseDTO(String token, UserLoginDetailsDTO user) {
        this.token = token;
        this.user = user;
    }


    public JwtResponseDTO(String message) {
        this.message = message;

    }

}
