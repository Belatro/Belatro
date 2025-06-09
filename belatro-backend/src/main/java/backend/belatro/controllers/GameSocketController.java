package backend.belatro.controllers;

import backend.belatro.dtos.*;
import backend.belatro.enums.MoveType;
import backend.belatro.events.GameStartedEvent;
import backend.belatro.events.GameStateChangedEvent;
import backend.belatro.events.TurnStartedEvent;
import backend.belatro.pojo.gamelogic.BelotGame;
import backend.belatro.pojo.gamelogic.Bid;
import backend.belatro.pojo.gamelogic.Card;
import backend.belatro.pojo.gamelogic.Player;
import backend.belatro.services.BelotGameService;
import backend.belatro.services.IMatchService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
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
    @EventListener // This method will be called when a GameStartedEvent is published
    public void handleGameStartedEvent(GameStartedEvent event) {
        System.out.println("Received GameStartedEvent for gameId: " + event.getGameId()); // For logging
        fanOutGameState(event.getGameId());
    }
    @EventListener
    public void onGameStateChanged(GameStateChangedEvent evt) {
        fanOutGameState(evt.gameId());
    }

    private void fanOutGameState(BelotGame game) {
        String gameId = game.getGameId();

        PublicGameView pub = svc.toPublicView(game);
        bus.convertAndSend("/topic/games/" + gameId, pub);

        // private view for each player
        game.getTurnOrder().forEach(player -> {
            PrivateGameView prv = svc.toPrivateView(game, player);
            bus.convertAndSendToUser(
                    player.getId(),                // username
                    "/queue/games/" + gameId,
                    prv
            );
        });
    }
    public void fanOutGameState(String gameId) {
        BelotGame game = svc.get(gameId);   // fetch once
        if (game != null) {
            fanOutGameState(game);          // reuse the helper above
        } else {
            System.err.println("Cannot fan out game state: Game not found with ID " + gameId);
        }
    }


    @MessageMapping("/games/{id}/play")
    public void play(@DestinationVariable String id, PlayCardMsg msg) {

        BelotGame game = svc.playCard(id, msg.playerId(), msg.card(), msg.declareBela());
        svc.save(game);
        matchService.recordMove(id,
                MoveType.PLAY_CARD,
                Map.of("playerId", msg.playerId(),
                        "card",     cardToString(msg.card()),
                        "declareBela", msg.declareBela()),
                0.0);

        fanOutGameState(game);
    }

    @MessageMapping("/games/{id}/bid")
    public void bid(@DestinationVariable String id, BidMsg msg) {

        Bid bid = msg.pass()
                ? Bid.pass(new Player(msg.playerId()))
                : Bid.callTrump(new Player(msg.playerId()), msg.trump());

        BelotGame game = svc.placeBid(id, bid);
        svc.save(game);
        Map<String,Object> payload = msg.pass()
                ? Map.of("playerId", msg.playerId(),
                "pass",     true)
                : Map.of("playerId", msg.playerId(),
                "pass",     false,
                "trump",    msg.trump());

        matchService.recordMove(id, MoveType.BID, payload, 0.0);

        fanOutGameState(game);
    }
    @MessageMapping("/games/{id}/challenge")
    public void challenge(@DestinationVariable String id, ChallengeMsg msg) {

        BelotGameService.ChallengeOutcome res = svc.challengeHand(id, msg.playerId());

        // write the move with the extra field
        matchService.recordMove(
                id,
                MoveType.CHALLENGE,
                Map.of("playerId", msg.playerId(),
                        "success",  res.success()),
                0.0);

        fanOutGameState(res.game());
    }

    @MessageMapping("/games/{id}/refresh")
    public void refresh(@DestinationVariable String id, Principal p) {
        BelotGame g = svc.get(id);
        // ① public
        bus.convertAndSend("/topic/games/" + id, svc.toPublicView(g));

        // ② private (only back to the caller)
        Player me = g.findPlayerById(p.getName());
        bus.convertAndSendToUser(
                p.getName(), "/queue/games/" + id,
                svc.toPrivateView(g, me));
    }
    // Sends public game state when a client subscribes to the public topic
    @SubscribeMapping("/topic/games/{gameId}")
    public PublicGameView handlePublicSubscription(@DestinationVariable String gameId) {
        BelotGame game = svc.get(gameId);
        if (game == null) {
            System.err.println("Cannot provide public view on subscription: Game not found with ID - " + gameId);
            // Consider how to handle this: throw error, return null, or specific error DTO
            return null;
        }
        System.out.println("Servicing public subscription to /topic/games/" + gameId);
        return svc.toPublicView(game);
    }

    // Sends private game state when a user subscribes to their private queue
    // Client subscribes to "/user/queue/games/{gameId}"
    // Spring maps this to "/queue/games/{gameId}" for the handler method if user destination prefix is /user/
    @SubscribeMapping("/queue/games/{gameId}")
    public PrivateGameView handlePrivateSubscription(@DestinationVariable String gameId, Principal principal) {
        if (principal == null || principal.getName() == null) {
            System.err.println("Cannot provide private view on subscription: Principal is null for gameId " + gameId);
            // Handle error: user is not authenticated
            return null; // Or throw an exception/error DTO
        }
        String username = principal.getName();
        BelotGame game = svc.get(gameId);
        if (game == null) {
            System.err.println("Cannot provide private view on subscription: Game not found with ID - " + gameId + " for user " + username);
            return null;
        }

        Player currentPlayer = game.findPlayerById(username); // Use existing helper

        if (currentPlayer == null) {
            System.err.println("Player " + username + " not found in game " + gameId + " for private view subscription.");
            // This case should ideally not happen if the user is part of the game
            return null;
        }
        System.out.println("Servicing private subscription to /user/queue/games/" + gameId + " for user " + username);
        return svc.toPrivateView(game, currentPlayer);
    }

    @MessageMapping("/games/{id}/cancel")
    public void cancel(@DestinationVariable String id, CancelMsg body,
                       Principal principal) {

        BelotGame g = svc.cancelMatch(id, principal.getName());
        fanOutGameState(g);

        // ️✂️  server-side disconnect for every session in this room
        bus.convertAndSend(
                "/topic/games/" + id,
                "DISCONNECT");     // or just rely on clients to close voluntarily
    }
    @EventListener
    public void onTurnStarted(TurnStartedEvent evt) {
        fanOutGameState(evt.matchId());   // just reuse the helper you already have
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
