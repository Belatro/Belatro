package backend.belatro.models;

import backend.belatro.enums.lobbyStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Document(collection = "lobbies")
public class Lobbies {
    @Id
    private String id;
    private String name;

    @DBRef
    private User hostUser;

    @DBRef
    private List<User> teamAPlayers;

    @DBRef
    private List<User> teamBPlayers;

    @DBRef
    private List<User> unassignedPlayers;

    private String gameMode;
    private lobbyStatus status;
    private Date createdAt;
    private boolean privateLobby;
    private String password;
}
