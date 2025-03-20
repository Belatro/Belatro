package backend.belatro.controllers;

import backend.belatro.dtos.CreateFriendshipDTO;
import backend.belatro.enums.FriendshipStatus;
import backend.belatro.models.Friendship;
import backend.belatro.services.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friendship")
public class FriendshipController {

    private final FriendshipService friendshipService;

    @Autowired
    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @GetMapping("/getAllFriendships")
    public ResponseEntity<List<Friendship>> getAllFriendships() {
        List<Friendship> friendships = friendshipService.getAllFriendships();
        return ResponseEntity.ok(friendships);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Friendship> getFriendshipById(@PathVariable String id) {
        Friendship friendship = friendshipService.getFriendshipById(id);
        return ResponseEntity.ok(friendship);
    }

    @GetMapping("/getAllByUserId/{userId}")
    public ResponseEntity<List<Friendship>> getFriendshipsByUser(@PathVariable String userId) {
        List<Friendship> friendships = friendshipService.getFriendshipsByUser(userId);
        return ResponseEntity.ok(friendships);
    }
    @PostMapping
    public ResponseEntity<Friendship> createFriendship(@RequestBody CreateFriendshipDTO dto) {
        Friendship createdFriendship = friendshipService.createFriendship(dto);
        return ResponseEntity.ok(createdFriendship);
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Friendship> acceptFriendship(@PathVariable String id) {
        Friendship updatedFriendship = friendshipService.updateFriendshipStatus(id, FriendshipStatus.ACCEPTED);
        return ResponseEntity.ok(updatedFriendship);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Friendship> rejectFriendship(@PathVariable String id) {
        Friendship updatedFriendship = friendshipService.updateFriendshipStatus(id, FriendshipStatus.REJECTED);
        return ResponseEntity.ok(updatedFriendship);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Friendship> cancelFriendship(@PathVariable String id) {
        Friendship updatedFriendship = friendshipService.updateFriendshipStatus(id, FriendshipStatus.CANCELLED);
        return ResponseEntity.ok(updatedFriendship);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFriendship(@PathVariable String id) {
        friendshipService.deleteFriendship(id);
        return ResponseEntity.noContent().build();
    }
}
