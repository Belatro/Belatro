package backend.belatro.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

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
}