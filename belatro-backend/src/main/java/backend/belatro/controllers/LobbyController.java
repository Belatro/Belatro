package backend.belatro.controllers;

import backend.belatro.dtos.*;
import backend.belatro.services.LobbyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lobbies")
public class LobbyController {
    private final LobbyService lobbyService;

    @Autowired
    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }
    @GetMapping
    public ResponseEntity<Iterable<LobbyDTO>> getAllLobbies() {
        Iterable<LobbyDTO> lobbies = lobbyService.getAllLobbies();
        return ResponseEntity.ok(lobbies);
    }

    @PostMapping
    public ResponseEntity<LobbyDTO> createLobby(@RequestBody LobbyDTO lobbyDTO) {
        LobbyDTO created = lobbyService.createLobby(lobbyDTO);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/join")
    public ResponseEntity<LobbyDTO> joinLobby(@RequestBody JoinLobbyRequestDTO joinRequest) {
        LobbyDTO updated = lobbyService.joinLobby(joinRequest);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{lobbyId}")
    public ResponseEntity<LobbyDTO> getLobby(@PathVariable String lobbyId) {
        LobbyDTO lobby = lobbyService.getLobby(lobbyId);
        return ResponseEntity.ok(lobby);
    }
    @GetMapping("/open")
    public ResponseEntity<Iterable<LobbyDTO>> getAllOpenLobbies() {
        Iterable<LobbyDTO> lobbies = lobbyService.getAllOpenLobbies();
        return ResponseEntity.ok(lobbies);
    }

    @PutMapping
    public ResponseEntity<LobbyDTO> updateLobby(@RequestBody LobbyDTO lobbyDTO) {
        LobbyDTO updated = lobbyService.updateLobby(lobbyDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{lobbyId}")
    public ResponseEntity<Void> deleteLobby(@PathVariable String lobbyId) {
        lobbyService.deleteLobby(lobbyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/switchTeam")
    public ResponseEntity<LobbyDTO> switchTeam(@RequestBody TeamSwitchRequestDTO switchRequest) {
        LobbyDTO updated = lobbyService.switchTeam(switchRequest);
        return ResponseEntity.ok(updated);
    }
    @PostMapping("/{lobbyId}/start-match")
    public ResponseEntity<MatchDTO> startMatch(@PathVariable String lobbyId) {
        MatchDTO matchDTO = lobbyService.startMatch(lobbyId);
        return ResponseEntity.ok(matchDTO);
    }
    @PatchMapping("/{lobbyId}/kick")
    public ResponseEntity<LobbyDTO> kickPlayer(@PathVariable String lobbyId,
                                               @RequestBody KickPlayerRequestDTO body) {
        LobbyDTO dto = lobbyService.kickPlayer(
                lobbyId,
                body.requesterUsername(),
                body.usernameToKick());
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/{lobbyId}/leave")
    public ResponseEntity<LobbyDTO> leaveLobby(@PathVariable String lobbyId,
                                               @RequestBody LeaveLobbyRequestDTO body) {
        return lobbyService.leaveLobby(lobbyId, body.username())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

}
