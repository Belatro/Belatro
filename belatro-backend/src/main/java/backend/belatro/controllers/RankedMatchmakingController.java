package backend.belatro.controllers;

import backend.belatro.services.MatchmakingService;
import backend.belatro.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.AccessDeniedException;

@RestController
@RequestMapping("/ranked")
@RequiredArgsConstructor
public class RankedMatchmakingController {

    private final MatchmakingService mmService;
    private final UserService userService;  // supplies current user

    @PostMapping("/queue")
    public void join() throws AccessDeniedException {
        mmService.joinQueue(userService.currentUser());
    }

    @DeleteMapping("/queue")
    public void leave() throws AccessDeniedException {
        mmService.leaveQueue(userService.currentUser());
    }

}