// Java
package backend.belatro.services;

import backend.belatro.annotations.GameAction;
import backend.belatro.dtos.BidDTO;
import backend.belatro.dtos.PlayerPublicInfo;
import backend.belatro.dtos.PrivateGameView;
import backend.belatro.dtos.PublicGameView;
import backend.belatro.enums.MoveType;
import backend.belatro.events.GameStartedEvent;
import backend.belatro.events.GameStateChangedEvent;
import backend.belatro.events.TurnStartedEvent;
import backend.belatro.models.User;
import backend.belatro.pojo.gamelogic.*;
import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.GameState;
import backend.belatro.pojo.gamelogic.enums.Rank;
import backend.belatro.repos.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BelotGameService {

    private static final String KEY_PREFIX = "belot:game:";
    private static final Logger LOGGER = LoggerFactory.getLogger(BelotGameService.class);
    private static final int TARGET_SCORE = 1001;

    private final RedisTemplate<String, BelotGame> redis;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepo userRepository;
    private final IMatchService matchService;
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.locks.ReentrantLock> GAME_LOCKS = new java.util.concurrent.ConcurrentHashMap<>();
    private static java.util.concurrent.locks.ReentrantLock lockFor(String gameId) {
        return GAME_LOCKS.computeIfAbsent(gameId, id -> new java.util.concurrent.locks.ReentrantLock());
    }

    @Autowired
    public BelotGameService(RedisTemplate<String, BelotGame> redis,
                            ApplicationEventPublisher eventPublisher,
                            UserRepo userRepository,
                            IMatchService matchService) {
        this.redis = redis;
        this.eventPublisher = eventPublisher;
        this.userRepository = userRepository;
        this.matchService = matchService;
    }

    public BelotGame start(String gameId, Team teamA, Team teamB) {
        BelotGame game = new BelotGame(gameId, teamA, teamB);
        game.setHandCompletionCallback(this::recordHandEnd);

        game.startGame();
        save(game);

        eventPublisher.publishEvent(new TurnStartedEvent(
                gameId,
                game.getCurrentLead().getId(),
                GameState.BIDDING));

        eventPublisher.publishEvent(new GameStartedEvent(this, gameId));
        LOGGER.info("Published GameStartedEvent for gameId: {}", gameId);

        return game;
    }

    @GameAction
    public ChallengeOutcome challengeHand(String gameId, String playerId) {
        var lock = lockFor(gameId);
        lock.lock();
        try {
            BelotGame g = getOrThrow(gameId);
            boolean ok = g.challengeHand(playerId);
            save(g);

            boolean challengerIsA =
                    g.getTeamA().getPlayers().stream().anyMatch(p -> p.getId().equals(playerId));
            String violatingTeam = challengerIsA ? "B" : "A";
            matchService.recordMove(
                    gameId,
                    MoveType.CHALLENGE,
                    Map.of(
                            "playerId", playerId,
                            "success", ok,
                            "violatingTeam", violatingTeam
                    ),
                    0.0
            );

            return new ChallengeOutcome(g, ok);
        } finally {
            lock.unlock();
        }
    }

    public BelotGame get(String gameId) {
        return redis.opsForValue().get(KEY_PREFIX + gameId);
    }

    @GameAction
    public BelotGame playCard(String gameId,
                              String playerId,
                              Card card,
                              boolean declareBela) {

        var lock = lockFor(gameId);
        lock.lock();
        try {
            BelotGame game = getOrThrow(gameId);
            Player p = game.findPlayerById(playerId);
            boolean isLegal = game.isValidPlay(p, card);
            boolean isTurn = (game.getGameState() == GameState.PLAYING)
                                       && game.getCurrentPlayer() != null
                                       && p.getId().equals(game.getCurrentPlayer().getId());

            if (!isTurn) {
                LOGGER.warn("Rejected out-of-turn play by {}", playerId);
                return game; // do not record
            }

            int beforeTricks = game.getCompletedTricks().size();

            boolean accepted;
            try {
                accepted = game.playCard(p, card, declareBela);
            } catch (RuntimeException ex) {
                LOGGER.warn("Domain rejected play by {}: {}", playerId, ex.getMessage());
                return game; // do not record
            }
            if (!accepted) {
                LOGGER.warn("Domain refused play by {} for card {}", playerId, card);
                return game; // do not record
            }

            save(game);

            // Record PLAY_CARD
            Map<String, Object> payload = new HashMap<>();
            payload.put("playerId", playerId);
            payload.put("card", card.toString());
            payload.put("declareBela", declareBela);
            payload.put("legal", isLegal);
            matchService.recordMove(gameId, MoveType.PLAY_CARD, payload, 0.0);

            // If trick completed, record END_TRICK with winner and points
            int afterTricks = game.getCompletedTricks().size();
            if (afterTricks > beforeTricks) {
                Trick last = game.getCompletedTricks().get(game.getCompletedTricks().size() - 1);
                String winnerId = last.determineWinner();
                int trickPoints = last.calculatePoints(); // raw card points (no +10 here)

                matchService.recordMove(
                        gameId,
                        MoveType.END_TRICK,
                        Map.of(
                                "winnerId", winnerId,
                                "points", trickPoints
                        ),
                        0.0
                );
            }

            if (game.getCurrentPlayer() != null) {
                eventPublisher.publishEvent(new TurnStartedEvent(
                        game.getGameId(),
                        game.getCurrentPlayer().getId(),
                        GameState.PLAYING
                ));
            }

            return game;
        } finally {
            lock.unlock();
        }
    }


    @GameAction
    public BelotGame placeBid(String gameId, Bid bid) {
        var lock = lockFor(gameId);
        lock.lock();
        try {
            BelotGame game = getOrThrow(gameId);
            Player p = game.findPlayerById(bid.getPlayer().getId());

            boolean isTurn = game.getGameState() == GameState.BIDDING
                    && p.getId().equals(game.getCurrentLead().getId());
            if (!isTurn) {
                LOGGER.warn("Rejected out-of-turn bid by {}", p.getId());
                return game; // do not record
            }

            boolean placed = game.placeBid(bid);
            save(game);

            if (placed) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("playerId", bid.getPlayer().getId());
                payload.put("pass", bid.isPass());
                if (bid.isTrumpCall() && bid.getSelectedTrump() != null) {
                    payload.put("trump", bid.getSelectedTrump().name());
                }
                matchService.recordMove(gameId, MoveType.BID, payload, 0.0);
            }

            // still bidding → announce next bidder
            if (game.getGameState() == GameState.BIDDING) {
                eventPublisher.publishEvent(new TurnStartedEvent(
                        gameId,
                        game.getCurrentLead().getId(),
                        GameState.BIDDING
                ));
            }

            return game;
        } finally {
            lock.unlock();
        }
    }

    public void save(BelotGame game) {
        BelotGame before = redis.opsForValue().get(KEY_PREFIX + game.getGameId());

        redis.opsForValue().set(KEY_PREFIX + game.getGameId(), game);

        if (before != null && before.getGameState() != game.getGameState()) {
            eventPublisher.publishEvent(new GameStateChangedEvent(game.getGameId()));
        }

        boolean justFinished = game.getGameState() == GameState.COMPLETED
                && (before == null || before.getGameState() != GameState.COMPLETED);

        if (justFinished) {
            String winnerLine = String.format(
                    "%s wins %d–%d",
                    game.getTeamAScore() > game.getTeamBScore() ? "Team A" : "Team B",
                    game.getTeamAScore(), game.getTeamBScore());

            matchService.finaliseMatch(game.getGameId(), winnerLine, Instant.now());

            // grace period for UI to fetch final states
            redis.expire(KEY_PREFIX + game.getGameId(), Duration.ofMinutes(3));
        }

        // If gameplay returned to bidding (new hand), notify who starts bidding
        if (before != null
                && before.getGameState() != GameState.BIDDING
                && game.getGameState() == GameState.BIDDING) {

            eventPublisher.publishEvent(new TurnStartedEvent(
                    game.getGameId(),
                    game.getCurrentLead().getId(),
                    GameState.BIDDING));
        }
    }

    private BelotGame getOrThrow(String gameId) {
        BelotGame g = get(gameId);
        if (g == null) {
            throw new IllegalStateException("Game not found: " + gameId);
        }
        g.setHandCompletionCallback(this::recordHandEnd);
        return g;
    }

    public PublicGameView toPublicView(BelotGame g) {
        List<BidDTO> dtos = g.getBids().stream()
                .map(b -> new BidDTO(
                        b.getPlayer().getId(),
                        b.getAction().name(),
                        b.getSelectedTrump()))
                .toList();
        List<BidDTO> safe = new ArrayList<>(dtos);

        List<PlayerPublicInfo> teamAList = g.getTeamA()
                .getPlayers().stream()
                .map(p -> {
                    Optional<User> maybeUser = userRepository.findById(p.getId());
                    String username = maybeUser.map(User::getUsername).orElse(p.getId());
                    return new PlayerPublicInfo(username, p.getId(), p.getHand().size());
                })
                .collect(Collectors.toList());

        List<PlayerPublicInfo> teamBList = g.getTeamB()
                .getPlayers().stream()
                .map(p -> {
                    Optional<User> maybeUser = userRepository.findById(p.getId());
                    String username = maybeUser.map(User::getUsername).orElse(p.getId());
                    return new PlayerPublicInfo(username, p.getId(), p.getHand().size());
                })
                .collect(Collectors.toList());

        Map<String, Boolean> challenged =
                g.getPlayers().stream()
                        .collect(Collectors.toMap(Player::getId, g::hasPlayerChallenged));

        String winner = (g.getGameState() == GameState.COMPLETED)
                ? g.getWinnerTeamId()
                : null;

        boolean tieBrk = (g.getTeamAScore() >= TARGET_SCORE
                && g.getTeamBScore() >= TARGET_SCORE
                && g.getTeamAScore() == g.getTeamBScore());

        List<PlayerPublicInfo> seatingOrder = g.getTurnOrder().stream()
                .map(p -> {
                    Optional<User> maybeUser = userRepository.findById(p.getId());
                    String username = maybeUser.map(User::getUsername).orElse(p.getId());
                    return new PlayerPublicInfo(username, p.getId(), p.getHand().size());
                })
                .toList();

        Map<String, backend.belatro.dtos.DeclarationsDTO> declarations =
                g.getPlayers().stream().collect(Collectors.toMap(
                        Player::getId,
                        pl -> buildDeclarations(pl, g.getTrump())
                ));

        Map<String, Boolean> belaDeclaredByPlayer = g.getPlayers().stream()
                .collect(Collectors.toMap(Player::getId, pl -> g.hasBelaDeclared(pl.getId())));

        Trick trickForDisplay = getTrickForDisplay(g);

        return new PublicGameView(
                g.getGameId(),
                g.getGameState(),
                safe,
                trickForDisplay,
                g.getTeamAScore(),
                g.getTeamBScore(),
                teamAList,
                teamBList,
                challenged,
                winner,
                tieBrk,
                seatingOrder,
                declarations,
                belaDeclaredByPlayer
        );
    }

    private static Trick getTrickForDisplay(BelotGame g) {
        Trick trickForDisplay = g.getCurrentTrick();

        if (trickForDisplay == null
                || (trickForDisplay.getPlays().isEmpty() && !g.getCompletedTricks().isEmpty())) {

            if (!g.getCompletedTricks().isEmpty()) {
                trickForDisplay = g.getCompletedTricks().getLast();
            } else {
                trickForDisplay = Trick.empty();
            }
        }
        return trickForDisplay;
    }

    public PrivateGameView toPrivateView(BelotGame g, Player p) {
        boolean yourTurn =
                g.getGameState() == GameState.BIDDING
                        ? g.getCurrentLead().getId().equals(p.getId())
                        : g.getCurrentPlayer() != null
                        && g.getCurrentPlayer().getId().equals(p.getId());
        boolean challengeUsed = g.hasPlayerChallenged(p);

        List<Boja> suitOrder = new ArrayList<>(EnumSet.allOf(Boja.class));
        int seed = p.getHand().stream()
                .mapToInt(c -> (c.getBoja().ordinal() << 4) | c.getRank().ordinal())
                .reduce(17, (a, b) -> 31 * a + b);
        Collections.shuffle(suitOrder, new Random(seed));

        Map<Boja, Integer> suitIdx = new EnumMap<>(Boja.class);
        for (int i = 0; i < suitOrder.size(); i++) suitIdx.put(suitOrder.get(i), i);

        Map<Rank,Integer> SEQ = BelotRankComparator.getSequenceRankOrder();

        Comparator<Card> suitThenSeq =
                Comparator.comparingInt((Card c) -> suitIdx.get(c.getBoja()))
                        .thenComparingInt((Card c) -> SEQ.getOrDefault(c.getRank(), -1));

        List<Card> sortedHand = new ArrayList<>(p.getHand());
        sortedHand.sort(suitThenSeq);

        return new PrivateGameView(toPublicView(g), sortedHand, yourTurn, challengeUsed);
    }

    public record ChallengeOutcome(BelotGame game, boolean success) {}

    public BelotGame cancelMatch(String matchId, String callerId) {
        var lock = lockFor(matchId);
        lock.lock();
        try {
            BelotGame g = getOrThrow(matchId);
            g.cancelMatch();
            save(g);

            matchService.recordMove(matchId, MoveType.SYSTEM, Map.of("by", callerId), 0.0);

            return g;
        } finally {
            lock.unlock();
        }
    }

    private static backend.belatro.dtos.DeclarationsDTO buildDeclarations(Player pl, Boja trump) {
        boolean bela = (trump != null) && ZvanjaValidator.isBela(pl.getHand(), trump);
        var seqs = ZvanjaValidator.evaluateAllSequences(pl.getHand());
        int bestSeq = seqs.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int four = ZvanjaValidator.evaluateFourOfAKind(pl.getHand()).orElse(0);
        return new backend.belatro.dtos.DeclarationsDTO(bela, seqs, four, bestSeq);
    }

    // Callback target – invoked by BelotGame at the end of a hand
    public void recordHandEnd(String gameId,
                              int teamAHandPoints, int teamBHandPoints,
                              int teamADeclPoints, int teamBDeclPoints,
                              int teamATricksWon, int teamBTricksWon,
                              boolean padanje, boolean capot) {

        BelotGame game = getOrThrow(gameId); // to include current total scores

        int finalScoreA = game.getTeamAScore();
        int finalScoreB = game.getTeamBScore();
        Map<String, Object> payload = new HashMap<>();
        payload.put("teamAHandPoints", teamAHandPoints);
        payload.put("teamBHandPoints", teamBHandPoints);
        payload.put("teamADeclPoints", teamADeclPoints);
        payload.put("teamBDeclPoints", teamBDeclPoints);
        payload.put("teamATricksWon", teamATricksWon);
        payload.put("teamBTricksWon", teamBTricksWon);
        payload.put("padanje", padanje);
        payload.put("capot", capot);
        payload.put("finalScoreA", game.getTeamAScore());
        payload.put("finalScoreB", game.getTeamBScore());
        payload.put("finalTeamAScore", finalScoreA);
        payload.put("finalTeamBScore", finalScoreB);

        matchService.recordMove(gameId, MoveType.END_HAND, payload, 0.0);
    }
}