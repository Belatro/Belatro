package backend.belatro.dtos;

import backend.belatro.enums.lobbyStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
public class LobbyDTO {
    private String id;
    private String name;
    private String gameMode;
    private lobbyStatus status;
    private Date createdAt;
    private UserSimpleDTO hostUser;
    private List<UserSimpleDTO> teamAPlayers;
    private List<UserSimpleDTO> teamBPlayers;
    private List<UserSimpleDTO> unassignedPlayers;
    private boolean privateLobby;
    private String password;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserSimpleDTO {
        private String id;
        private String username;
    }
}
