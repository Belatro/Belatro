package backend.belatro.controllers;

import backend.belatro.dtos.BidMsg;
import backend.belatro.dtos.PlayCardMsg;
import backend.belatro.dtos.PrivateGameView;
import backend.belatro.dtos.PublicGameView;
import backend.belatro.enums.MoveType;
import backend.belatro.pojo.gamelogic.BelotGame;
import backend.belatro.pojo.gamelogic.Bid;
import backend.belatro.pojo.gamelogic.Card;
import backend.belatro.pojo.gamelogic.Player;
import backend.belatro.services.BelotGameService;
import backend.belatro.services.IMatchService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.stream.Stream;

@Controller
public class GameSocketController {

    private final BelotGameService svc;
    private final SimpMessagingTemplate bus;
    private final IMatchService matchService;

    public GameSocketController(BelotGameService svc,
                                SimpMessagingTemplate bus, IMatchService matchService) {
        this.svc = svc;
        this.bus = bus;
        this.matchService = matchService;
    }

    @MessageMapping("/games/{id}/play")
    public void play(@DestinationVariable String id, PlayCardMsg msg) {

        BelotGame game = svc.playCard(id, msg.playerId(), msg.card(), msg.declareBela());

        matchService.recordMove(id,
                MoveType.PLAY_CARD,
                Map.of("playerId", msg.playerId(),
                        "card",     cardToString(msg.card()),
                        "declareBela", msg.declareBela()),
                0.0);

        fanOut(id, game);
    }

    @MessageMapping("/games/{id}/bid")
    public void bid(@DestinationVariable String id, BidMsg msg) {

        Bid bid = msg.pass()
                ? Bid.pass(new Player(msg.playerId()))
                : Bid.callTrump(new Player(msg.playerId()), msg.trump());

        BelotGame game = svc.placeBid(id, bid);

        matchService.recordMove(id,
                MoveType.BID,
                Map.of("playerId", msg.playerId(),
                        "pass",     msg.pass(),
                        "trump",    msg.trump()),
                0.0);

        fanOut(id, game);
    }

    private void fanOut(String gameId, BelotGame game) {
        PublicGameView pub = svc.toPublicView(game);
        bus.convertAndSend("/topic/games/" + gameId, pub);

        Stream.concat(game.getTeamA().getPlayers().stream(),
                        game.getTeamB().getPlayers().stream())
                .forEach(pl -> {
                    PrivateGameView prv = svc.toPrivateView(game, pl);
                    bus.convertAndSendToUser(pl.getId(),
                            "/queue/games/" + gameId,
                            prv);
                });
    }

    private static String cardToString(Card c) {
        return c == null ? "" : c.toString();   // uses your override
    }

}
