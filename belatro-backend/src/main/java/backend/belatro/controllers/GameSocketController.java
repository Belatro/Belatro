package backend.belatro.controllers;

import backend.belatro.dtos.BidMsg;
import backend.belatro.dtos.PlayCardMsg;
import backend.belatro.enums.MoveType;
import backend.belatro.pojo.gamelogic.BelotGame;
import backend.belatro.pojo.gamelogic.Bid;
import backend.belatro.pojo.gamelogic.Player;
import backend.belatro.services.BelotGameService;
import backend.belatro.services.IMatchService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

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
    public void play(@DestinationVariable String id,
                     PlayCardMsg msg) {

        BelotGame updated = svc.playCard(
                id,
                msg.playerId(),
                msg.card(),
                msg.declareBela());

        matchService.recordMove(
                id,
                MoveType.valueOf("PLAY_CARD"),
                Map.of("playerId",    msg.playerId(),
                        "card",        msg.card().toString(),
                        "declareBela", msg.declareBela()),
                /* evaluation */ 0.0);

        bus.convertAndSend("/topic/games/" + id, updated);
    }

    @MessageMapping("/games/{id}/bid")
    public void bid(@DestinationVariable String id,
                    BidMsg msg) {

        Bid bid = msg.pass()
                ? Bid.pass(new Player(msg.playerId()))
                : Bid.callTrump(new Player(msg.playerId()), msg.trump());

        BelotGame updated = svc.placeBid(id, bid);

        matchService.recordMove(
                id,
                MoveType.valueOf("BID"),
                Map.of("playerId", msg.playerId(),
                        "pass",     msg.pass(),
                        "trump",    msg.trump()),
                /* evaluation */ 0.0);

        bus.convertAndSend("/topic/games/" + id, updated);
    }

}
