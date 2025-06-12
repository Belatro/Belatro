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
import backend.belatro.pojo.gamelogic.enums.GameState;
import backend.belatro.repos.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BelotGameService {

    private static final String KEY_PREFIX = "belot:game:";
    private static final Logger LOGGER = LoggerFactory.getLogger(BelotGameService.class);
    private static final int TARGET_SCORE = 1001;
    private final RedisTemplate<String, BelotGame> redis;
    private final ApplicationEventPublisher eventPublisher; // Inject publisher
    private final UserRepo userRepository;
    private final IMatchService matchService;


    public BelotGameService(RedisTemplate<String, BelotGame> redis, ApplicationEventPublisher eventPublisher, UserRepo userRepository, IMatchService matchService) {
        this.redis = redis;
        this.eventPublisher = eventPublisher;
        this.userRepository = userRepository;
        this.matchService = matchService;
    }


    public BelotGame start(String gameId, Team teamA, Team teamB) {
        BelotGame game = new BelotGame(gameId, teamA, teamB);
        game.startGame(); // Deals cards, sets bidding phase, etc.
        save(game);
        eventPublisher.publishEvent(new TurnStartedEvent(
                gameId,
                game.getCurrentLead().getId(),
                GameState.BIDDING));

        eventPublisher.publishEvent(new GameStartedEvent(this, gameId));
        System.out.println("Published GameStartedEvent for gameId: " + gameId); // For logging

        return game;
    }

    @GameAction
    public ChallengeOutcome challengeHand(String gameId, String playerId) {
        BelotGame g = getOrThrow(gameId);
        boolean   ok = g.challengeHand(playerId);   // <- the flag
        save(g);
        return new ChallengeOutcome(g, ok);
    }


    public BelotGame get(String gameId) {
        return redis.opsForValue().get(KEY_PREFIX + gameId);
    }

    @GameAction
    public BelotGame playCard(String gameId,
                              String playerId,
                              Card card,
                              boolean declareBela) {

        BelotGame game = getOrThrow(gameId);
        Player p = game.findPlayerById(playerId);
        boolean isTurn = (game.getGameState() == GameState.BIDDING && p.getId().equals(game.getCurrentLead().getId()))
                || (game.getGameState() == GameState.PLAYING && game.getCurrentPlayer() != null
                && p.getId().equals(game.getCurrentPlayer().getId()));
        if (!isTurn) {
            LOGGER.warn("Rejected out-of-turn play by {}", playerId);
            return game; // or throw an exception to inform the client
        }

        game.playCard(p, card, declareBela);
        save(game);
        eventPublisher.publishEvent(new TurnStartedEvent(gameId,
                game.getCurrentPlayer().getId(),
                GameState.PLAYING));

        return game;
    }

    @GameAction
    public BelotGame placeBid(String gameId, Bid bid) {
        BelotGame game = getOrThrow(gameId);
        Player p = game.findPlayerById(bid.getPlayer().getId());
        boolean isTurn = game.getGameState() == GameState.BIDDING &&
                p.getId().equals(game.getCurrentLead().getId());
        if (!isTurn) {
            LOGGER.warn("Rejected out-of-turn bid by {}", p.getId());
            return game;
        }
        game.placeBid(bid);
        save(game);
        if (game.getGameState() == GameState.BIDDING) {
            eventPublisher.publishEvent(new TurnStartedEvent(gameId,
                    game.getCurrentLead().getId(),
                    GameState.BIDDING));
        }
        return game;
    }


    // BelotGameService

    public void save(BelotGame game) {
        BelotGame before = redis.opsForValue().get(KEY_PREFIX + game.getGameId());

        redis.opsForValue().set(KEY_PREFIX + game.getGameId(), game);

        if (before != null && before.getGameState() != game.getGameState()) {
            eventPublisher.publishEvent(new GameStateChangedEvent(game.getGameId()));
        }

        boolean justFinished = game.getGameState() == GameState.COMPLETED &&
                (before == null || before.getGameState() != GameState.COMPLETED);

        if (justFinished) {
            String winnerLine = String.format(
                    "%s wins %d–%d",
                    game.getTeamAScore() > game.getTeamBScore() ? "Team A" : "Team B",
                    game.getTeamAScore(), game.getTeamBScore());

            matchService.finaliseMatch(game.getGameId(), winnerLine, Instant.now());


            redis.expire(KEY_PREFIX + game.getGameId(), Duration.ofMinutes(3)); // grace period
        }


        if (before != null
                && before.getGameState() != GameState.BIDDING
                && game.getGameState()   == GameState.BIDDING) {

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
                    // Look up User by ID:
                    Optional<User> maybeUser = userRepository.findById(p.getId());
                    String username = maybeUser
                            .map(User::getUsername)
                            .orElse(p.getId()); // fallback to ID if user record missing

                    return new PlayerPublicInfo(
                            username,
                            p.getId(),
                            p.getHand().size()
                    );
                })
                .collect(Collectors.toList());

        // 3) Build teamB’s PlayerPublicInfo list:
        List<PlayerPublicInfo> teamBList = g.getTeamB()
                .getPlayers().stream()
                .map(p -> {
                    Optional<User> maybeUser = userRepository.findById(p.getId());
                    String username = maybeUser
                            .map(User::getUsername)
                            .orElse(p.getId());

                    return new PlayerPublicInfo(
                            username,
                            p.getId(),
                            p.getHand().size()
                    );
                })
                .collect(Collectors.toList());
        Map<String, Boolean> challenged =
                g.getPlayers().stream()
                        .collect(Collectors.toMap(Player::getId, g::hasPlayerChallenged));
        String winner = (g.getGameState() == GameState.COMPLETED)
                ? g.getWinnerTeamId()
                : null;

        boolean tieBrk = (g.getTeamAScore() >= TARGET_SCORE &&
                g.getTeamBScore() >= TARGET_SCORE &&
                g.getTeamAScore() == g.getTeamBScore());
        List<PlayerPublicInfo> seatingOrder = g.getTurnOrder().stream()
                .map(p -> {
                    Optional<User> maybeUser = userRepository.findById(p.getId());
                    String username = maybeUser.map(User::getUsername)
                            .orElse(p.getId());
                    return new PlayerPublicInfo(
                            username,
                            p.getId(),
                            p.getHand().size()
                    );
                })
                .toList();
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
                seatingOrder
        );
    }

    private static Trick getTrickForDisplay(BelotGame g) {
        Trick trickForDisplay = g.getCurrentTrick();

        /* --- NEW null-safe guard -------------------------------------------- */
        if (trickForDisplay == null ||
                (trickForDisplay.getPlays().isEmpty() && !g.getCompletedTricks().isEmpty())) {

            if (!g.getCompletedTricks().isEmpty()) {
                trickForDisplay = g.getCompletedTricks()
                        .getLast();
            } else {
                // brand-new game: create an empty Trick so the field is never null
                trickForDisplay = Trick.empty();   // ← Trick has a no-arg ctor
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

        return new PrivateGameView(
                toPublicView(g),
                p.getHand(),
                yourTurn,
                challengeUsed                   // NEW
        );
    }
    public record ChallengeOutcome(BelotGame game, boolean success) {}
    public BelotGame cancelMatch(String matchId, String callerId) {
        BelotGame g = getOrThrow(matchId);


        g.cancelMatch();
        save(g);

        matchService.recordMove(matchId, MoveType.SYSTEM,
                Map.of("by", callerId), 0.0);

        return g;
    }

}