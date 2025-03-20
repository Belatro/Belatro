package backend.belatro.models;

import backend.belatro.enums.lobbyStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collation = "lobbies")
public class Lobbies {
    @Id
    private String id;
    private String name;
    @DBRef
    private User hostUser;
    @DBRef
    private List<User> players;
    private String gameMode;
    private lobbyStatus status;

}
