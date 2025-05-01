package backend.belatro.models;

import backend.belatro.enums.GameMode;
import backend.belatro.pojo.Move;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Document(collection = "match")
public class Match {
    @Id
    private String id;
    @DBRef
    private List<User> TeamA;
    @DBRef
    private List<User> TeamB;
    @DBRef
    private Lobbies originLobby;
    private GameMode gameMode;
    private List<Move> moves;
    private String result;
    private Date startTime;
    private Date endTime;


}
