package backend.belatro.controllers;

import backend.belatro.dtos.UserUpdateDTO;
import backend.belatro.exceptions.UserNotFoundException;
import backend.belatro.models.User;
import backend.belatro.repos.UserRepo;
import backend.belatro.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/findAll")
    public ResponseEntity<List<User>> findAllUsers(){
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }
    @GetMapping("/{id}")
    public ResponseEntity<User> findUserById(@PathVariable String id){
        User user = userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id " + id));
        return ResponseEntity.ok(user);
    }
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user){
        User createdUser = userService.createUser(user);
        return ResponseEntity.ok(createdUser);
    }
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
}
