package backend.belatro.components;

import backend.belatro.pojo.gamelogic.BelotGame;
import backend.belatro.pojo.gamelogic.enums.GameState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class EmptyMatchReaper {

    private final RedisTemplate<String, BelotGame> redis;
    private final SimpUserRegistry simpUserRegistry;

    private static final Duration QUIET_FOR = Duration.ofMinutes(20);

    @Scheduled(fixedDelay = 300_000)    // every 5 min
    public void reap() {
        for (String key : redis.keys("game:*")) {
            BelotGame g = redis.opsForValue().get(key);
            assert g != null;
            boolean stillPlaying =
                    g.getGameState() != GameState.COMPLETED &&
                            g.getGameState() != GameState.CANCELLED;
            if (!stillPlaying) continue;

            boolean nobodyConnected =
                    simpUserRegistry.findSubscriptions(
                                    s -> s.getDestination().equals("/topic/games/" + g.getGameId()))
                            .isEmpty();

            boolean quietLongEnough =
                    Instant.now().minus(QUIET_FOR)
                            .isAfter(g.getLastActivity());  // you already update this field

            if (nobodyConnected && quietLongEnough) {
                g.cancelMatch();
                redis.opsForValue().set(key, g);
                // (optional) write audit line
            }
        }
    }
}
