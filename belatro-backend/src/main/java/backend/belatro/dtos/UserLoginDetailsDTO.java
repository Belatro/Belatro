package backend.belatro.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // Explicitly add a no-argument constructor
@AllArgsConstructor

public class UserLoginDetailsDTO {
    private String id;
    private String username;

}
