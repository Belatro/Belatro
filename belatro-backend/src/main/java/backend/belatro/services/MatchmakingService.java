package backend.belatro.services;

import backend.belatro.components.QueueCache;
import backend.belatro.dtos.LobbyDTO;
import backend.belatro.dtos.MatchDTO;
import backend.belatro.models.MatchmakingQueueEntry;
import backend.belatro.models.User;
import backend.belatro.repos.MatchmakingQueueRepo;
import backend.belatro.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import static backend.belatro.models.MatchmakingQueueEntry.Status.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchmakingService {

    private final MatchmakingQueueRepo queueRepo;
    private final LobbyService         lobbyService;
    private final SimpMessagingTemplate broker;
    private final QueueCache cache;
    private final ReentrantLock lock = new ReentrantLock(true);
    private final UserRepo userRepo;

    /** Tunables; move to application.yml if you like */
    private static final int   INITIAL_DIFF   = 100;
    private static final int   EXPAND_STEP    = 100;
    private static final Duration EXPAND_EVERY = Duration.ofSeconds(30);

    /* ---------- Public API ---------- */

    @Transactional
    public void joinQueue(User user) {
        // guard against duplicates
        queueRepo.findByUserIdAndStatus(user.getId(), QUEUED)
                .ifPresent(e -> { throw new IllegalStateException("Already queued"); });

        var entry = MatchmakingQueueEntry.builder()
                .userId(user.getId())
                .eloSnapshot(user.getEloRating())
                .queuedAt(Instant.now())
                .status(QUEUED)
                .build();

        queueRepo.save(entry);
        cache.add(entry);
        dispatch();     // try match right away
    }

    @Transactional
    public void leaveQueue(User user) {
        queueRepo.findByUserIdAndStatus(user.getId(), QUEUED).ifPresent(e -> {
            e.setStatus(CANCELLED);
            queueRepo.save(e);
            cache.remove(e);
        });
    }

    /* ---------- Scheduled dispatcher ---------- */

    /** Safety net: every 2 s we try matching (covers race conditions). */
    @Scheduled(fixedDelay = 2000)
    public void scheduledDispatch() { dispatch(); }

    /** Core matching algorithm (single-threaded via lock). */
    private void dispatch() {
        if (!lock.tryLock()) return;     // another thread is matching
        try {
            while (true) {
                List<MatchmakingQueueEntry> pool = cache.snapshot();
                if (pool.size() < 4) return;

                // oldest wait → controls expansion
                Instant oldest = pool.get(0).getQueuedAt();
                long waitedMs  = Duration.between(oldest, Instant.now()).toMillis();
                int allowed    = INITIAL_DIFF + (int)((waitedMs / EXPAND_EVERY.toMillis()) * EXPAND_STEP);

                int bestStart  = -1, bestSpread = Integer.MAX_VALUE;
                for (int i = 0; i <= pool.size() - 4; i++) {
                    int spread = pool.get(i+3).getEloSnapshot() - pool.get(i).getEloSnapshot();
                    if (spread <= allowed && spread < bestSpread) {
                        bestSpread = spread; bestStart = i;
                    }
                }
                if (bestStart == -1) return;  // nothing within threshold yet

                var four = pool.subList(bestStart, bestStart + 4);
                // sort inside the group
                four.sort(Comparator.comparingInt(MatchmakingQueueEntry::getEloSnapshot));
                User high  = user(four.get(3));
                User midHi = user(four.get(2));
                User midLo = user(four.get(1));
                User low   = user(four.get(0));

                List<User> teamA = List.of(high, low);
                List<User> teamB = List.of(midHi, midLo);

                log.debug("Team A chosen  : {}", teamA.stream()
                        .map(User::getUsername)
                        .toList());
                log.debug("Team B chosen  : {}", teamB.stream()
                        .map(User::getUsername)
                        .toList());
                // make lobby & match
                LobbyDTO lobby = lobbyService.createRankedLobby(teamA, teamB);
                MatchDTO match = lobbyService.startMatch(lobby.getId());

                log.debug("After createLobby → teamA.size={} teamB.size={} unassigned={}",
                        lobby.getTeamAPlayers().size(),
                        lobby.getTeamBPlayers().size(),
                        lobby.getUnassignedPlayers().size());

                four.forEach(e -> {
                    e.setStatus(MATCHED);
                    queueRepo.save(e);
                    cache.remove(e);
                });
                Stream.concat(teamA.stream(), teamB.stream())
                        .forEach(user ->
                                broker.convertAndSendToUser(
                                        user.getUsername(),           // principal name in the WS session
                                        "/queue/match-found",
                                        match));

                log.info("Ranked match formed: {} vs {} (matchId={})",
                        teamA.stream().map(User::getUsername).toList(),
                        teamB.stream().map(User::getUsername).toList(),
                        match.getId());
            }
        } finally { lock.unlock(); }
    }

    /* convenience to fetch User by ID (cache / DB as you like) */
    private User user(MatchmakingQueueEntry e) {
        return userRepo.findById(e.getUserId()).orElseThrow();
    }
}
