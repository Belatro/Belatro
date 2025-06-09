package backend.belatro.dtos;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class SignupRequestDTO {
    private String username;
    private String email;
    private String password;

    public SignupRequestDTO() {}

}
