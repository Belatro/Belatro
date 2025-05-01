package backend.belatro.controllers;

import backend.belatro.dtos.BidMsg;
import backend.belatro.dtos.PlayCardMsg;
import backend.belatro.pojo.gamelogic.BelotGame;
import backend.belatro.pojo.gamelogic.Bid;
import backend.belatro.pojo.gamelogic.Player;
import backend.belatro.services.BelotGameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameSocketController {

    private final BelotGameService svc;
    private final SimpMessagingTemplate bus;

    public GameSocketController(BelotGameService svc,
                                SimpMessagingTemplate bus) {
        this.svc = svc;
        this.bus = bus;
    }

    @MessageMapping("/games/{id}/play")
    public void play(@DestinationVariable String id,
                     PlayCardMsg msg) {

        BelotGame updated = svc.playCard(
                id,
                msg.playerId(),
                msg.card(),
                msg.declareBela());

        bus.convertAndSend("/topic/games/" + id, updated);
    }
    @MessageMapping("/games/{id}/bid")
    public void bid(@DestinationVariable String id, BidMsg msg) {

        // 1) Build a domain Bid object
        Bid bid = msg.pass()
                ? Bid.pass(new Player(msg.playerId()))             // static helper
                : Bid.callTrump(new Player(msg.playerId()), msg.trump());

        // 2) Apply it through the service (loads, mutates, saves)
        BelotGame g = svc.placeBid(id, bid);

        // 3) Broadcast the updated game state to all subscribers
        bus.convertAndSend("/topic/games/" + id, g);
    }
}
