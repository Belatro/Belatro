package backend.belatro.services;

import backend.belatro.dtos.BidDTO;
import backend.belatro.dtos.PrivateGameView;
import backend.belatro.dtos.PublicGameView;
import backend.belatro.pojo.gamelogic.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BelotGameService {

    private static final String KEY_PREFIX = "belot:game:";

    private final RedisTemplate<String, BelotGame> redis;

    public BelotGameService(RedisTemplate<String, BelotGame> redis) {
        this.redis = redis;
    }


    public BelotGame start(String gameId, Team teamA, Team teamB) {
        BelotGame game = new BelotGame(gameId, teamA, teamB);
        game.startGame();
        save(game);
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
        return new PrivateGameView(
                toPublicView(g),
                p.getHand(),
                g.getCurrentPlayer().equals(p));
    }
}
