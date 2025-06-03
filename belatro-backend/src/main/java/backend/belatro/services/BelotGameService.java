package backend.belatro.services;

import backend.belatro.dtos.BidDTO;
import backend.belatro.dtos.PrivateGameView;
import backend.belatro.dtos.PublicGameView;
import backend.belatro.events.GameStartedEvent;
import backend.belatro.events.GameStateChangedEvent;
import backend.belatro.pojo.gamelogic.*;
import backend.belatro.pojo.gamelogic.enums.GameState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BelotGameService {

    private static final String KEY_PREFIX = "belot:game:";
    private static final Logger LOGGER = LoggerFactory.getLogger(BelotGameService.class);
    private final RedisTemplate<String, BelotGame> redis;
    private final ApplicationEventPublisher eventPublisher; // Inject publisher

    public BelotGameService(RedisTemplate<String, BelotGame> redis, ApplicationEventPublisher eventPublisher) {
        this.redis = redis;
        this.eventPublisher = eventPublisher;
    }


    public BelotGame start(String gameId, Team teamA, Team teamB) {
        BelotGame game = new BelotGame(gameId, teamA, teamB);
        game.startGame(); // Deals cards, sets bidding phase, etc.
        save(game);

        // Publish an event after the game is started and saved
        eventPublisher.publishEvent(new GameStartedEvent(this, gameId));
        System.out.println("Published GameStartedEvent for gameId: " + gameId); // For logging

        return game;
    }



    public BelotGame get(String gameId) {
        return redis.opsForValue().get(KEY_PREFIX + gameId);
    }


    public BelotGame playCard(String gameId,
                              String playerId,
                              Card card,
                              boolean declareBela) {

        BelotGame game = getOrThrow(gameId);
        Player     p   = game.findPlayerById(playerId);   // helper already in your POJO

        game.playCard(p, card, declareBela);
        save(game);
        return game;
    }


    public BelotGame placeBid(String gameId, Bid bid) {
        BelotGame game = getOrThrow(gameId);
        game.placeBid(bid);
        save(game);
        return game;
    }



    public void save(BelotGame game) {
        redis.opsForValue()
                .set(KEY_PREFIX + game.getGameId(), game);
        eventPublisher.publishEvent(new GameStateChangedEvent(game.getGameId()));
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

        return new PublicGameView(
                g.getGameId(),
                g.getGameState(),
                safe,
                g.getCurrentTrick(),
                g.getTeamAScore(),
                g.getTeamBScore()
        );
    }

    public PrivateGameView toPrivateView(BelotGame g, Player p) {
        boolean yourTurn =
                g.getGameState() == GameState.BIDDING
                        ? g.getCurrentLead().getId().equals(p.getId())
                        : g.getCurrentPlayer() != null
                        && g.getCurrentPlayer().getId().equals(p.getId());


        return new PrivateGameView(
                toPublicView(g),
                p.getHand(),
                yourTurn);
    }
}
