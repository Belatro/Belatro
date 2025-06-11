package backend.belatro.services;

import backend.belatro.dtos.MatchDTO;
import backend.belatro.dtos.RematchCancelledEvent;
import backend.belatro.dtos.RematchStartedEvent;
import backend.belatro.dtos.RematchVoteEvent;
import backend.belatro.pojo.gamelogic.BelotGame;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RematchService {

    private static final String PREFIX = "belot:rematch:";
    private final RedisTemplate<String, String> redis;
    private final BelotGameService gameSvc;
    private final IMatchService    matchSvc;
    private final ApplicationEventPublisher publisher;

    /* ------------- public API used by controllers ------------------------- */

    public void requestOrAccept(String oldGameId, String playerId) {
        String keyAccepted = PREFIX + oldGameId + ":accepted";
        redis.opsForSet().add(keyAccepted, playerId);          // self-vote
        redis.expire(keyAccepted, Duration.ofMinutes(2));      // GC

        publisher.publishEvent(new RematchVoteEvent(
                oldGameId, getAccepted(oldGameId)));

        checkAndMaybeStart(oldGameId);
    }

    public void decline(String oldGameId, String playerId) {
        publisher.publishEvent(new RematchCancelledEvent(oldGameId, playerId));
        cleanKeys(oldGameId);
    }

    /* ------------- helpers ------------------------------------------------ */

    private Set<String> getAccepted(String oldGameId) {
        return redis.opsForSet().members(
                PREFIX + oldGameId + ":accepted");
    }

    private void cleanKeys(String oldGameId) {
        redis.delete(List.of(
                PREFIX + oldGameId + ":accepted",
                PREFIX + oldGameId + ":state"));
    }

    private void checkAndMaybeStart(String oldGameId) {

        BelotGame oldGame = gameSvc.get(oldGameId);
        if (oldGame == null) return;         // safety

        if (getAccepted(oldGameId).size() < oldGame.getPlayers().size()) return;

        /* everyone accepted → spin up a fresh match */
        MatchDTO oldMatch = matchSvc.getMatch(oldGameId);       // DTO copy
        MatchDTO clone    = cloneForRematch(oldMatch);          // strip id / timings
        MatchDTO newMatch = matchSvc.createMatch(clone);        // :contentReference[oaicite:0]{index=0}

        gameSvc.start(newMatch.getId(),
                oldGame.getTeamA(),
                oldGame.getTeamB());                      // :contentReference[oaicite:1]{index=1}

        publisher.publishEvent(new RematchStartedEvent(
                oldGameId, newMatch.getId()));

        cleanKeys(oldGameId);      // job done
    }

    private MatchDTO cloneForRematch(MatchDTO src) {
        MatchDTO dto = new MatchDTO();
        dto.setGameMode(src.getGameMode());
        dto.setTeamA(src.getTeamA());
        dto.setTeamB(src.getTeamB());
        dto.setOriginLobby(src.getOriginLobby());
        dto.setStartTime(new Date());
        return dto;
    }
}
