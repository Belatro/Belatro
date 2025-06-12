package backend.belatro;

import backend.belatro.configs.RedisConfig;
import backend.belatro.pojo.gamelogic.BelotGame;
import backend.belatro.pojo.gamelogic.Deck;
import backend.belatro.pojo.gamelogic.Player;
import backend.belatro.pojo.gamelogic.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a complete BelotGame object graph can be
 * written to Redis and deserialised unchanged.
 */
@DataRedisTest                 // loads only Redis infrastructure
@Import(RedisConfig.class)     // brings in belotGameRedisTemplate
@ActiveProfiles("test")          // switch to an in-memory DB, test Redis port, etc.
class BelotGameRedisRoundTripTest {

    /** The template configured for BelotGame values (see RedisConfig). */
    @Autowired
    private RedisTemplate<String, BelotGame> belotGameRedisTemplate;

    private String redisKey;     // keep for clean-up

    @AfterEach
    void tearDown() {
        if (redisKey != null) {
            belotGameRedisTemplate.delete(redisKey);
        }
    }

    @Test
    void belotGame_roundTripsThroughRedis() {
        // ── given ────────────────────────────────────────────────────────────────
        BelotGame before = buildSampleGame();
        redisKey = "belot:test:" + before.getGameId();

        // ── when ────────────────────────────────────────────────────────────────
        belotGameRedisTemplate.opsForValue().set(redisKey, before);
        BelotGame after = belotGameRedisTemplate.opsForValue().get(redisKey);

        // ── then ────────────────────────────────────────────────────────────────
        assertThat(after)
                .as("everything should survive serialise → Redis → deserialise")
                .usingRecursiveComparison()
                .ignoringFields(
                        "deck",                 // shuffles ⇒ new order
                        "random",               // transient helper
                        "currentTrickPlays"     // derived getter
                )
                .isEqualTo(before);

    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static BelotGame buildSampleGame() {
        // players
        Player p1 = new Player("Alice");
        Player p2 = new Player("Bob");
        Player p3 = new Player("Carol");
        Player p4 = new Player("Dave");

        // teams
        Team teamA = new Team(List.of(p1, p3));
        Team teamB = new Team(List.of(p2, p4));

        // fresh deck (all 32 cards) and a game ID
        Deck deck = new Deck();
        String gameId = UUID.randomUUID().toString();

        // your domain constructor that Jackson is also allowed to call
        BelotGame game = new BelotGame(gameId, teamA, teamB);
        game.startGame();

        return game;
    }
}
