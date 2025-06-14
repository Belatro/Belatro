package backend.belatro.services;

import backend.belatro.components.QueueCache;
import backend.belatro.dtos.LobbyDTO;
import backend.belatro.dtos.MatchDTO;
import backend.belatro.dtos.QueueStatusDTO;
import backend.belatro.enums.QueueState;
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
    private final QueueCache           cache;
    private final ReentrantLock        lock = new ReentrantLock(true);
    private final UserRepo             userRepo;

    // Tunables
    private static final int      INITIAL_DIFF = 100;
    private static final int      EXPAND_STEP  = 100;
    private static final Duration EXPAND_EVERY = Duration.ofSeconds(10);

    /* ─────────────────────────────────────────────────────────────── */
    /* PUBLIC API                                                     */
    /* ─────────────────────────────────────────────────────────────── */

    @Transactional
    public void joinQueue(User user) {
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

        // ★ new – initial status push
        pushStatus(user, QueueState.IN_QUEUE, estimateWaitSeconds(user), cache.size(), null);

        dispatch();   // try to form match instantly
    }

    @Transactional
    public void leaveQueue(User user) {
        queueRepo.findByUserIdAndStatus(user.getId(), QUEUED).ifPresent(e -> {
            e.setStatus(CANCELLED);
            queueRepo.save(e);
            cache.remove(e);

            // ★ new – cancelled frame
            pushStatus(user, QueueState.CANCELLED, 0, cache.size(), null);
        });
    }

    /* ─────────────────────────────────────────────────────────────── */
    /* SCHEDULED TICK                                                 */
    /* ─────────────────────────────────────────────────────────────── */

    /** Fires every 2 s – tries to build matches *and* updates ETAs. */
    @Scheduled(fixedDelay = 2000)
    public void scheduledDispatch() {
        // ★ new – broadcast IN_QUEUE frames to everyone still waiting
        cache.snapshot().forEach(e -> {
            User u = user(e);
            pushStatus(u,
                    QueueState.IN_QUEUE,
                    estimateWaitSeconds(u),
                    cache.size(),
                    null);
        });

        dispatch();   // existing matching logic
    }

    /* ─────────────────────────────────────────────────────────────── */
    /* MATCHING CORE                                                  */
    /* ─────────────────────────────────────────────────────────────── */

    private void dispatch() {
        if (!lock.tryLock()) return;
        try {
            while (true) {
                List<MatchmakingQueueEntry> pool = cache.snapshot();
                if (pool.size() < 4) return;

                Instant oldest = pool.get(0).getQueuedAt();
                long    waited = Duration.between(oldest, Instant.now()).toMillis();
                int     allowed = INITIAL_DIFF +
                        (int) ((waited / EXPAND_EVERY.toMillis()) * EXPAND_STEP);

                int bestStart = -1, bestSpread = Integer.MAX_VALUE;
                for (int i = 0; i <= pool.size() - 4; i++) {
                    int spread = pool.get(i + 3).getEloSnapshot() - pool.get(i).getEloSnapshot();
                    if (spread <= allowed && spread < bestSpread) {
                        bestSpread = spread;
                        bestStart  = i;
                    }
                }
                if (bestStart == -1) return;  // nothing close enough yet

                var four = pool.subList(bestStart, bestStart + 4);
                four.sort(Comparator.comparingInt(MatchmakingQueueEntry::getEloSnapshot));

                User high  = user(four.get(3));
                User midHi = user(four.get(2));
                User midLo = user(four.get(1));
                User low   = user(four.get(0));

                List<User> teamA = List.of(high, low);
                List<User> teamB = List.of(midHi, midLo);

                LobbyDTO lobby  = lobbyService.createRankedLobby(teamA, teamB);
                MatchDTO match  = lobbyService.startMatch(lobby.getId());

                four.forEach(e -> {
                    e.setStatus(MATCHED);
                    queueRepo.save(e);
                    cache.remove(e);
                });

                // ★ new – push MATCH_FOUND + legacy /match-found frame
                Stream.concat(teamA.stream(), teamB.stream()).forEach(u -> {
                    pushStatus(u, QueueState.MATCH_FOUND, 0, cache.size(), match.getId());
                    broker.convertAndSendToUser(
                            u.getUsername(),
                            "/queue/match-found",
                            match);
                });

                log.info("Ranked match formed: {} vs {} (matchId={})",
                        teamA.stream().map(User::getUsername).toList(),
                        teamB.stream().map(User::getUsername).toList(),
                        match.getId());
            }
        } finally { lock.unlock(); }
    }

    /* ─────────────────────────────────────────────────────────────── */
    /* SUPPORT                                                        */
    /* ─────────────────────────────────────────────────────────────── */

    private void pushStatus(User u,
                            QueueState state,
                            int etaSeconds,
                            int queueSize,
                            String matchId) {

        QueueStatusDTO dto = new QueueStatusDTO(
                state,
                etaSeconds,
                queueSize,
                u.getEloRating(),
                matchId);

        broker.convertAndSendToUser(
                u.getUsername(),
                "/queue/ranked/status",
                dto);
    }

    /** Very light ETA estimate: how many 30 s expansions until a match is *likely*. */
    private int estimateWaitSeconds(User u) {
        List<MatchmakingQueueEntry> pool = cache.snapshot();
        if (pool.size() < 4) return -1;

        // assume player will pair with three closest mmr neighbours
        pool.sort(Comparator.comparingInt(MatchmakingQueueEntry::getEloSnapshot));
        int idx = -1;
        for (int i = 0; i < pool.size(); i++)
            if (pool.get(i).getUserId().equals(u.getId())) { idx = i; break; }
        if (idx == -1) return -1;

        int left = Math.max(idx - 3, 0);
        int right = Math.min(left + 3, pool.size() - 1);
        int spread = pool.get(right).getEloSnapshot() - pool.get(left).getEloSnapshot();

        // time until allowedDiff >= spread
        int diffNeeded = spread - INITIAL_DIFF;
        if (diffNeeded <= 0) return 5;   // should pop next tick

        int expansions = (int) Math.ceil(diffNeeded / (double) EXPAND_STEP);
        return expansions * (int) EXPAND_EVERY.getSeconds();
    }

    private User user(MatchmakingQueueEntry e) {
        return userRepo.findById(e.getUserId()).orElseThrow();
    }
}

