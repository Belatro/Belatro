package backend.belatro.models;

import backend.belatro.pojo.Move;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Document(collation = "matches")
public class Match {
    @Id
    private String id;
    @DBRef
    private List<User> players;
    @DBRef
    private Lobbies lobbyId;
    private String gameMode;
    private List<Move> moves;
    private String result;
    private Date startTime;
    private Date endTime;


}
