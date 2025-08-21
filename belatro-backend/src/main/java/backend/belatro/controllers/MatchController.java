package backend.belatro.controllers;

import backend.belatro.dtos.HandDTO;
import backend.belatro.dtos.MatchDTO;
import backend.belatro.dtos.MoveDTO;
import backend.belatro.services.IMatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches")
public class MatchController {

    private final IMatchService matchService;

    public MatchController(IMatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<MatchDTO> createMatch(@RequestBody MatchDTO matchDTO) {
        MatchDTO createdMatch = matchService.createMatch(matchDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(createdMatch);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchDTO> getMatch(@PathVariable String id) {
        MatchDTO match = matchService.getMatch(id);
        return match != null ? ResponseEntity.ok(match)
                : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<MatchDTO>> getAllMatches() {
        List<MatchDTO> matches = matchService.getAllMatches();
        return ResponseEntity.ok(matches);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MatchDTO> updateMatch(@PathVariable String id, @RequestBody MatchDTO matchDTO) {
        try {
            MatchDTO updatedMatch = matchService.updateMatch(id, matchDTO);
            return ResponseEntity.ok(updatedMatch);
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable String id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/moves")
    public ResponseEntity<List<MoveDTO>> getMoves(@PathVariable String id) {
        return ResponseEntity.ok(matchService.getMoves(id));
    }

    @GetMapping("/{id}/structured-moves")
    public ResponseEntity<List<HandDTO>> getStructuredMoves(@PathVariable String id) {
        return ResponseEntity.ok(matchService.getStructuredMoves(id));
    }

    @GetMapping("/getmatchbylobbyid/{lobbyId}")
    public ResponseEntity<MatchDTO> getMatchByLobbyId(@PathVariable String lobbyId) {
        MatchDTO match = matchService.getMatchByLobbyId(lobbyId);
        return match != null ? ResponseEntity.ok(match)
                : ResponseEntity.notFound().build();
    }

}
