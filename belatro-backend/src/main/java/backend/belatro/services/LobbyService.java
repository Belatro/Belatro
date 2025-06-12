package backend.belatro.services;

import backend.belatro.dtos.JoinLobbyRequestDTO;
import backend.belatro.dtos.LobbyDTO;
import backend.belatro.dtos.MatchDTO;
import backend.belatro.dtos.TeamSwitchRequestDTO;
import backend.belatro.models.User;

import java.util.List;
import java.util.Optional;

public interface LobbyService {
    LobbyDTO kickPlayer(String lobbyId,
                        String requesterUsername,
                        String usernameToKick);

    Optional<LobbyDTO> leaveLobby(String lobbyId,
                                  String username);

    LobbyDTO createLobby(LobbyDTO lobbyDTO);
    LobbyDTO getLobby(String lobbyId);
    LobbyDTO updateLobby(LobbyDTO lobbyDTO);
    void deleteLobby(String lobbyId);
    LobbyDTO joinLobby(JoinLobbyRequestDTO joinRequest);
    LobbyDTO switchTeam(TeamSwitchRequestDTO switchRequest);
    MatchDTO startMatch(String lobbyId);
    List<LobbyDTO> getAllLobbies();
    List<LobbyDTO> getAllOpenLobbies();

    LobbyDTO createRankedLobby(List<User> teamA, List<User> teamB);
}
