package backend.belatro.Integration;

import backend.belatro.models.User;
import backend.belatro.repos.MatchmakingQueueRepo;
import backend.belatro.repos.UserRepo;
import backend.belatro.services.MatchmakingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static backend.belatro.models.MatchmakingQueueEntry.Status.QUEUED;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@TestPropertySource(properties =
        "logging.level.backend.belatro.services.MatchmakingService=DEBUG")
class MatchmakingIntegrationTest {

    @Container                                   // spins up once for the class
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource                        // point Spring-Data to TC URI
    static void mongoProps(DynamicPropertyRegistry r) {
        r.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired MatchmakingService   mm;
    @Autowired MatchmakingQueueRepo repo;
    @Autowired
    UserRepo userRepo;   // add to the test class

    /* --------------------------------------------------------- */

    @Test
    void fourPlayersFormOneMatch() {
        User u1 = user("p1", 1000);
        User u2 = user("p2", 1010);
        User u3 = user("p3", 995);
        User u4 = user("p4", 1005);

        userRepo.saveAll(List.of(u1, u2, u3, u4));   //  ← add this line


        mm.joinQueue(u1);
        mm.joinQueue(u2);
        mm.joinQueue(u3);
        mm.joinQueue(u4);

        // Awaitility optional here if your dispatcher runs on a schedule
        Awaitility.await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() ->
                        assertThat(repo.findByStatus(QUEUED)).isEmpty());
    }

    /* ---------- helpers (tiny stubs) ---------- */
    private static User user(String username, int elo) {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setUsername(username);
        u.setEloRating(elo);
        u.setGamesPlayed(20);
        return u;
    }
}
