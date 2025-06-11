package backend.belatro.models;

import backend.belatro.enums.Role;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Set;

@Data

@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String username;
    private String email;
    private String passwordHashed;
    private int eloRating;
    private int level;
    private int expPoints;
    private Date lastLogin;
    private int gamesPlayed;
    private Set<Role> roles = Set.of(Role.ROLE_USER);
    private boolean deletionRequested = false;
}