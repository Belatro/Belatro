package backend.belatro.dtos;

import lombok.Data;

@Data
public class TeamSwitchRequestDTO {
    private String lobbyId;
    private String userId;
    private String targetTeam;
}
