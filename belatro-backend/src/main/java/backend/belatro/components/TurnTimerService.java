package backend.belatro.components;

import backend.belatro.enums.MoveType;
import backend.belatro.events.GameStateChangedEvent;
import backend.belatro.events.TurnStartedEvent;
import backend.belatro.pojo.gamelogic.BelotGame;
import backend.belatro.pojo.gamelogic.Bid;
import backend.belatro.pojo.gamelogic.Card;
import backend.belatro.pojo.gamelogic.Player;
import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.GameState;
import backend.belatro.services.BelotGameService;
import backend.belatro.services.IMatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

import static backend.belatro.configs.SecurityConfig.log;

@Component

public class TurnTimerService {

    private final TaskScheduler scheduler;          // comes from Spring Boot
    private final SimpUserRegistry userRegistry;       // tracks live WS sessions
    private final RedisTemplate<String, BelotGame> redis;    // same bean you already use
    private final BelotGameService gameService;        // to call playCard
    private final IMatchService matchService;
    // for recordMove + fanOut

    public static final String KEY_PREFIX = "belot:game:";
    
    @Autowired
    public TurnTimerService(@Qualifier("taskScheduler") TaskScheduler scheduler, SimpUserRegistry userRegistry, RedisTemplate<String, BelotGame> redis,
                            BelotGameService gameService, IMatchService matchService) {
        this.scheduler = scheduler;
        this.userRegistry = userRegistry;
        this.redis = redis;
        this.gameService = gameService;
        this.matchService = matchService;   
    }
    /** keep at most one timer per matchId */
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    private static final Duration TURN_TIMEOUT = Duration.ofMillis(30000);

    @EventListener
    public void onGameStateChanged(GameStateChangedEvent e) {

        // load the freshest copy of the game
        BelotGame g = redis.opsForValue().get(KEY_PREFIX + e.gameId());
        if (g == null) return;          // should not happen

        GameState s = g.getGameState();
        if (s == GameState.COMPLETED || s == GameState.CANCELLED) {
            cancelTimer(e.gameId());    // tear down any pending 30-s task
        }
    }
    @EventListener
    public void onTurnStarted(TurnStartedEvent e) {
        log.info("⏱  Scheduling turn-timer for match={} player={}",
                e.matchId(), e.playerId());
        Optional.ofNullable(timers.remove(e.matchId()))
                .ifPresent(f -> f.cancel(false));
        // schedule new one
        ScheduledFuture<?> f = scheduler.schedule(
                () -> onTimeout(e.matchId(), e.playerId()),
                new Date(System.currentTimeMillis() + TURN_TIMEOUT.toMillis())
        );

        timers.put(e.matchId(), f);
    }

    /* ------------------------------------------------------------------ */
    public void scheduleTurn(String matchId, String playerId) {

        // 1️⃣ cancel any previous timer for this match
        Optional.ofNullable(timers.remove(matchId)).ifPresent(f -> f.cancel(false));

        // 2️⃣ schedule new 30-second task  (use java.util.Date, not Instant)
        ScheduledFuture<?> fut = scheduler.schedule(
                () -> onTimeout(matchId, playerId),
                new Date(System.currentTimeMillis() + TURN_TIMEOUT.toMillis())
        );
        timers.put(matchId, fut);
    }

    /* ------------------------------------------------------------------ */
    private void onTimeout(String matchId, String playerId) {
        log.warn("⏰  Timer expired for match={} player={}", matchId, playerId);

        /* 1️⃣ is the player still online? --------------------------------- */
        Set<SimpSubscription> subs = userRegistry.findSubscriptions(
                s -> s.getDestination().equals("/topic/games/" + matchId));

        SimpUser simpUser = userRegistry.getUser(playerId);
        boolean connected = simpUser != null && simpUser.getSessions().stream()
                .flatMap(s -> s.getSubscriptions().stream())
                .anyMatch(sub -> sub.getDestination().equals("/topic/games/" + matchId));

        if (!connected) {
            log.info("⛔ Auto-action aborted: {} not connected to {}", playerId, matchId);
            return;
        }               // don’t punish disconnected users

        /* 2️⃣ load the game and check we’re still on that exact turn ------ */
        String key   = KEY_PREFIX + matchId;         // same prefix everywhere
        BelotGame game = redis.opsForValue().get(key);
        if (game == null ) return; // stale timer
        boolean stillTurn =
                (game.getGameState() == GameState.BIDDING  &&
                        playerId.equals(game.getCurrentLead().getId()))
                        || (game.getGameState() == GameState.PLAYING  &&
                        playerId.equals(game.getCurrentPlayer().getId()));
        if (!stillTurn) {
            log.info("⛔ Auto-action aborted: not {}’s turn any more", playerId);
            return;       // someone has already bid / played
        }

        switch (game.getGameState()) {

            /* ===== PLAYING: already implemented (auto-card) =============== */
            case PLAYING -> autoPlayCard(game, matchId, playerId);

            /* ===== BIDDING: new auto-PASS logic =========================== */
            case BIDDING -> autoPassBid(game, matchId, playerId);

            /* ===== anything else: ignore timer ============================ */
            default -> { /* no action */ }
        }
    }

    /* ------------------------------------------------------------------ */
    public void cancelTimer(String matchId) {
        Optional.ofNullable(timers.remove(matchId)).ifPresent(f -> f.cancel(false));
    }
    private void autoPassBid(BelotGame game,
                             String matchId,
                             String playerId) {

        // ① *use the Player instance from playerId*, not currentLead() again
        Player bidder = game.findPlayerById(playerId);
        if (bidder == null) return;               // safety

        // ② dealer-must-call check unchanged
        boolean secondRound  = game.getBids().size() >= game.getTurnOrder().size();
        boolean dealerForced =
                bidder.equals(game.getDealer()) &&
                        game.getBids().size() == game.getTurnOrder().size() - 1;   // 3 passes already

        if (dealerForced) {
            Boja trump = randomTrump();
            Bid call   = Bid.callTrump(bidder, trump);
            BelotGame updated = gameService.placeBid(matchId, call);
            recordAutoBid(matchId, playerId, "CALL_TRUMP", trump.name());

            /* schedule timer for the *first card* – now in PLAYING phase */
            scheduleTurn(matchId, updated.getCurrentPlayer().getId());
            return;
        }

        // ③ normal auto-PASS
        Bid pass = Bid.pass(bidder);
        BelotGame updated = gameService.placeBid(matchId, pass);
        recordAutoBid(matchId, playerId, "PASS", null);

        /* if bidding continues, schedule for the next lead */
        if (updated.getGameState() == GameState.BIDDING) {
            scheduleTurn(matchId, updated.getCurrentLead().getId());
        } else {                                     // bidding finished → PLAYING
            scheduleTurn(matchId, updated.getCurrentPlayer().getId());
        }
    }


    private void autoPlayCard(BelotGame game,
                              String matchId,
                              String playerId) {

        // still that player's turn?
        if (!playerId.equals(game.getCurrentPlayer().getId())) return;

        /* ---- pick a random legal card ----------------------------------- */
        List<Card> legal = game.getCurrentPlayer()
                .getHand().stream()
                .filter(c -> game.isValidPlay(game.getCurrentPlayer(), c))
                .toList();
        if (legal.isEmpty()) return;                 // should never happen

        Card chosen = legal.get(ThreadLocalRandom.current().nextInt(legal.size()));

        /* ---- play it through normal service path ------------------------ */
        gameService.playCard(matchId, playerId, chosen, false);      // false ⇒ no bela
        matchService.recordMove(matchId, MoveType.PLAY_CARD,
                Map.of("playerId", playerId,
                        "card", chosen.toString()), 0.0);

        /* timer for next player handled inside playCard() */
    }

    /* helper */
    private void recordAutoBid(String matchId, String pid, String kind, String trump) {
        Map<String,Object> payload = new HashMap<>();
        payload.put("playerId", pid);
        payload.put("bid", kind);
        if (trump != null) payload.put("trump", trump);
        matchService.recordMove(matchId, MoveType.BID, payload, 0.0);
    }

    private Boja randomTrump() {
        return Boja.values()[ThreadLocalRandom.current().nextInt(Boja.values().length)];
    }

}

