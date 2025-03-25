package backend.belatro.controllers;

import backend.belatro.dtos.MatchDTO;
import backend.belatro.services.IMatchService;
import org.springframework.http.HttpStatus;
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
        return new ResponseEntity<>(createdMatch, HttpStatus.CREATED);
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
}
