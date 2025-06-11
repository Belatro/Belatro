package backend.belatro.Integration;

import backend.belatro.context.StompTestClient;
import backend.belatro.context.TestWebSocketMessageSecurity;
import backend.belatro.dtos.MatchDTO;
import backend.belatro.models.User;
import backend.belatro.repos.UserRepo;
import backend.belatro.services.MatchmakingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestWebSocketMessageSecurity.class})          // the test-only security cfg
class WsNotificationTest {

    @LocalServerPort int port;

    @Autowired MatchmakingService mm;
    @Autowired UserRepo          userRepo;             // ← we’ll use this directly

    @Test
    void playerReceivesMatchFound() throws Exception {

        // --- 1) create & persist four players -----------------------------
        User u1 = user("p1", 1000);
        User u2 = user("p2", 1010);
        User u3 = user("p3", 995);
        User u4 = user("p4", 1005);
        userRepo.saveAll(List.of(u1, u2, u3, u4));

        // --- 2) open WS for just the first user ----------------------------
        String wsUrl = "ws://localhost:" + port
                + "/ws/websocket?user=" + u1.getUsername();
        try (StompTestClient client = new StompTestClient(wsUrl)) {

            BlockingQueue<MatchDTO> inbox =
                    client.subscribe("/user/queue/match-found", MatchDTO.class);

            // --- 3) queue all four players --------------------------------
            mm.joinQueue(u1);
            mm.joinQueue(u2);
            mm.joinQueue(u3);
            mm.joinQueue(u4);

            // --- 4) wait for the message (up to 10 s) ----------------------
            MatchDTO msg = inbox.poll(10, TimeUnit.SECONDS);

            assertThat(msg).isNotNull();       // ✅ will pass when match forms
            Assertions.assertNotNull(msg);
            assertThat(msg.getId()).isNotBlank();
        }
    }

    // --- helper to build a User -------------------------------------------
    private static User user(String username, int elo) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setUsername(username);
        u.setEloRating(elo);
        u.setGamesPlayed(20);
        return u;
    }
}



