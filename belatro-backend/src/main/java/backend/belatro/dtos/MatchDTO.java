package backend.belatro.dtos;

import backend.belatro.enums.GameMode;
import backend.belatro.pojo.Move;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class MatchDTO {
    private String id;
    private List<UserUpdateDTO> teamA;
    private List<UserUpdateDTO> teamB;
    private LobbyDTO originLobby;
    private GameMode gameMode;
    private List<Move> moves;
    private String result;
    private Date startTime;
    private Date endTime;
}
