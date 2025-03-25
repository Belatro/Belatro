package backend.belatro.dtos;

import lombok.Data;

@Data
public class JoinLobbyRequestDTO {
    private String lobbyId;
    private String userId;
    private String password;
}
