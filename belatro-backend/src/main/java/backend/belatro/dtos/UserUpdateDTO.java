package backend.belatro.dtos;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class UserUpdateDTO {
    private String username;
    private String email;
    private String passwordHashed;
    private int eloRating;
    private int level;
    private int expPoints;
    private Date lastLogin;
}
