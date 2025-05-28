package backend.belatro.controllers;

import backend.belatro.dtos.*;
import backend.belatro.enums.MoveType;
import backend.belatro.pojo.gamelogic.*;
import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.Rank;
import backend.belatro.services.BelotGameService;
import backend.belatro.services.IMatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GameSocketControllerTest {

    @Mock  BelotGameService      svc;
    @Mock  SimpMessagingTemplate bus;
    @Mock  IMatchService         matchSvc;
    @InjectMocks
    GameSocketController ctrl;

    private BelotGame game;
    private Player a, b, c, d;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        a = new Player("Alice");
        b = new Player("Bob");
        c = new Player("Carol");
        d = new Player("Dave");

        game = new BelotGame("g42",
                new Team(List.of(a, c)),
                new Team(List.of(b, d)));

        // service stubs for DTO mapping
        when(svc.toPublicView(any()))
                .thenReturn(new PublicGameView(null,null,null,null,0,0));
        when(svc.toPrivateView(any(), any()))
                .thenReturn(new PrivateGameView(null, List.of(), false));
    }

    /* ------------------------------------------------------------------ */
    /*  play() handler                                                    */
    /* ------------------------------------------------------------------ */

    @Test
    void play_broadcastsPublicAndPrivateViews() {
        // arrange
        PlayCardMsg msg = new PlayCardMsg("Alice",
                new Card(Boja.HERC, Rank.KRALJ), false);
        when(svc.playCard(eq("g42"), anyString(), any(Card.class), anyBoolean()))
                .thenReturn(game);

        // act
        ctrl.play("g42", msg);

        // assert ─ public payload exactly once
        verify(bus).convertAndSend(
                eq("/topic/games/g42"),
                any(PublicGameView.class));

        // assert ─ four private payloads, one per player
        verify(bus, times(4)).convertAndSendToUser(
                anyString(),                       // playerId matcher
                eq("/queue/games/g42"),            // destination matcher
                any(PrivateGameView.class));       // payload matcher

        // move recorded & service invoked once
        verify(matchSvc).recordMove(
                eq("g42"),
                eq(MoveType.PLAY_CARD),
                any(Map.class),
                eq(0.0));

        verify(svc).playCard(eq("g42"),
                eq("Alice"), any(Card.class), eq(false));
    }

    /* ------------------------------------------------------------------ */
    /*  bid() handler                                                     */
    /* ------------------------------------------------------------------ */

    @Test
    void bid_broadcastsPublicAndPrivateViews() {
        // arrange
        BidMsg msg = new BidMsg("Bob", false, Boja.PIK);
        when(svc.placeBid(eq("g42"), any(Bid.class))).thenReturn(game);

        // act
        ctrl.bid("g42", msg);

        // assert ─ public payload
        verify(bus).convertAndSend(
                eq("/topic/games/g42"),
                any(PublicGameView.class));

        // assert ─ four private payloads
        verify(bus, times(4)).convertAndSendToUser(
                anyString(),
                eq("/queue/games/g42"),
                any(PrivateGameView.class));

        // move recorded & service invoked
        verify(matchSvc).recordMove(
                eq("g42"),
                eq(MoveType.BID),
                any(Map.class),
                eq(0.0));

        verify(svc).placeBid(eq("g42"), any(Bid.class));
    }
}
