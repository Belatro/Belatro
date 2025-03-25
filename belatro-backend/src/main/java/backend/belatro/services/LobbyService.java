package backend.belatro.services;

import backend.belatro.dtos.JoinLobbyRequestDTO;
import backend.belatro.dtos.LobbyDTO;
import backend.belatro.dtos.MatchDTO;
import backend.belatro.dtos.TeamSwitchRequestDTO;

public interface LobbyService {
    LobbyDTO createLobby(LobbyDTO lobbyDTO);
    LobbyDTO getLobby(String lobbyId);
    LobbyDTO updateLobby(LobbyDTO lobbyDTO);
    void deleteLobby(String lobbyId);
    LobbyDTO joinLobby(JoinLobbyRequestDTO joinRequest);
    LobbyDTO switchTeam(TeamSwitchRequestDTO switchRequest);
    MatchDTO startMatch(String lobbyId);
}
