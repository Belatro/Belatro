package backend.belatro.controllers;

import backend.belatro.dtos.PlayerMatchHistoryDTO;
import backend.belatro.dtos.PlayerMatchSummaryDTO;
import backend.belatro.dtos.UserUpdateDTO;
import backend.belatro.exceptions.UserNotFoundException;
import backend.belatro.models.User;
import backend.belatro.services.IMatchService;
import backend.belatro.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final IMatchService matchService;

    @Autowired
    public UserController(UserService userService, IMatchService matchService) {
        this.userService = userService;
        this.matchService = matchService;
    }

    @GetMapping("/findAll")
    public ResponseEntity<List<User>> getAllUsers(){
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable String id){
        User user = userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + id));
        return ResponseEntity.ok(user);
    }
    /*
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user){
        User createdUser = userService.createUser(user);
        return ResponseEntity.ok(createdUser);
    }
    */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable String id, @RequestBody UserUpdateDTO updateDTO){
        User updatedUser = userService.updateUser(id, updateDTO);
        return ResponseEntity.ok(updatedUser);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id){
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{playerId}/history")
    public ResponseEntity<Page<PlayerMatchHistoryDTO>> history(
            @PathVariable String playerId,
            @RequestParam(defaultValue="0")  int page,
            @RequestParam(defaultValue="20") int size
    ) {
        // Sort only — don’t filter on endTime here
        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "endTime")
        );
        Page<PlayerMatchHistoryDTO> results =
                matchService.getFinishedMatchHistoryByPlayer(playerId, pageable);
        return ResponseEntity.ok(results);
    }
    @GetMapping("/{playerId}/history/summary")
    public ResponseEntity<Page<PlayerMatchSummaryDTO>> getSummaryByPlayer(
            @PathVariable String playerId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pg = PageRequest.of(page, size);
        Page<PlayerMatchSummaryDTO> summaries =
                matchService.getMatchSummariesByPlayer(playerId, pg);
        return ResponseEntity.ok(summaries);
    }
    @PostMapping("/{id}/request-forget")
    public ResponseEntity<Void> requestForget(@PathVariable String id) {
        userService.requestAccountDeletionById(id);
        return ResponseEntity.accepted().build();
    }

}
