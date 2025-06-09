package backend.belatro.controllers;

import backend.belatro.dtos.RematchCancelledEvent;
import backend.belatro.dtos.RematchStartedEvent;
import backend.belatro.dtos.RematchVoteEvent;
import backend.belatro.services.RematchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;


/**
 * STOMP endpoints and broadcasts for the “Rematch?” flow.
 *
 * Clients:
 *   • subscribe  → /topic/games/{gameId}/rematch
 *   • send vote  → /app/games/{gameId}/rematch/vote
 *   • send decline → /app/games/{gameId}/rematch/decline
 *
 * Payloads sent back:
 *   { "type":"VOTE",   "accepted":[ ... ] }
 *   { "type":"START",  "newGameId":"…"    }
 *   { "type":"CANCEL", "by":"playerId"    }
 */
@Controller

public class RematchSocketController {

    private final RematchService rematchService;
    private final SimpMessagingTemplate broker;
    @Autowired
    public RematchSocketController(RematchService rematchService, SimpMessagingTemplate broker) {
        this.rematchService = rematchService;
        this.broker = broker;
    }

    /* -------------------- incoming from players -------------------- */

    @MessageMapping("/games/{gameId}/rematch/vote")
    public void vote(@DestinationVariable String gameId,
                     Principal principal) {
        rematchService.requestOrAccept(gameId, principal.getName());
    }

    @MessageMapping("/games/{gameId}/rematch/decline")
    public void decline(@DestinationVariable String gameId,
                        Principal principal) {
        rematchService.decline(gameId, principal.getName());
    }

    /* -------------------- outbound broadcasts ---------------------- */

    @EventListener
    public void onVote(RematchVoteEvent ev) {
        broker.convertAndSend("/topic/games/" + ev.oldGameId() + "/rematch",
                Map.of("type", "VOTE",
                        "accepted", ev.accepted()));
    }

    @EventListener
    public void onStarted(RematchStartedEvent ev) {
        broker.convertAndSend("/topic/games/" + ev.oldGameId() + "/rematch",
                Map.of("type", "START",
                        "newGameId", ev.newGameId()));
    }

    @EventListener
    public void onCancelled(RematchCancelledEvent ev) {
        broker.convertAndSend("/topic/games/" + ev.oldGameId() + "/rematch",
                Map.of("type", "CANCEL",
                        "by", ev.cancelledBy()));
    }
}
