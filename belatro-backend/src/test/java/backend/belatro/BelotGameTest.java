package backend.belatro;

import backend.belatro.pojo.gamelogic.BelotGame;
import backend.belatro.pojo.gamelogic.Bid;
import backend.belatro.pojo.gamelogic.Player;
import backend.belatro.pojo.gamelogic.Team;
import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BelotGameTest {

    private BelotGame game;
    private Team teamA;
    private Team teamB;
    private Player playerA1, playerA2;
    private Player playerB1, playerB2;

    @BeforeEach
    public void setup() {
        // Create players (assumed constructor takes a player ID)
        playerA1 = new Player("A1");
        playerA2 = new Player("A2");
        playerB1 = new Player("B1");
        playerB2 = new Player("B2");

        // Create teams (assumed constructor takes an ID and a list of players)
        teamA = new Team(List.of(playerA1, playerA2));
        teamB = new Team(List.of(playerB1, playerB2));

        // Create a new game instance
        game = new BelotGame("game1", teamA, teamB);
    }

    @Test
    public void testStartGame() {
        // Start the game; this should select a dealer, shuffle the deck, and deal the initial cards.
        game.startGame();

        // After starting, the game should be in the BIDDING state.
        assertEquals(GameState.BIDDING, game.getGameState(), "Game state should be BIDDING after starting");

        // Check that the talon has 2 cards.
        assertEquals(8, game.getTalon().size(), "Talon should contain 2 cards");

        // Verify that every player’s hand has been dealt 6 cards.
        for (Player player : List.of(playerA1, playerA2, playerB1, playerB2)) {
            assertEquals(6, player.getHand().size(), "Player " + player.getId() + " should have 6 cards initially");
        }

        // Check that both a dealer and a current player (lead) have been set.
        assertNotNull(game.getDealer(), "A dealer should be assigned");
        assertNotNull(game.getCurrentLead(), "A current player should be set");
    }

    @Test
    public void testPassBidAndTurnRotation() {
        game.startGame();

        // Simulate the current player passing.
        Player currentBidder = game.getCurrentPlayer();
        assertNotNull(currentBidder, "Current player should not be null");

        Bid passBid = Bid.pass(currentBidder);
        boolean bidProcessed = game.placeBid(passBid);

        assertTrue(bidProcessed, "The pass bid should be processed");

        // After a pass, the turn should rotate to the next player.
        Player newBidder = game.getCurrentPlayer();
        assertNotEquals(currentBidder, newBidder, "Turn should move to the next player after a pass");

        // Game should still be in BIDDING state
        assertEquals(GameState.BIDDING, game.getGameState(), "Game should still be in BIDDING state after a pass");
    }

    @Test
    public void testSuccessfulBidTrumpCall() {
        game.startGame();

        // Get the correct player whose turn it is to bid
        Player currentBidder = game.getCurrentPlayer();
        assertNotNull(currentBidder, "Current player should not be null");

        // Store initial hand size
        int initialHandSize = currentBidder.getHand().size();
        assertEquals(6, initialHandSize, "Players should have 6 cards initially");

        // Make a trump call
        Bid trumpCallBid = Bid.callTrump(currentBidder, Boja.HERC);
        boolean bidProcessed = game.placeBid(trumpCallBid);

        assertTrue(bidProcessed, "The trump call bid should be processed successfully");
        assertEquals(GameState.PLAYING, game.getGameState(), "Game state should be PLAYING after a successful trump call");
        assertEquals(Boja.HERC, game.getTrump(), "The trump should be set to HEARTS");

        // The trump caller should be set correctly
        assertEquals(currentBidder, game.getTrumpCaller(), "Trump caller should be set to the player who called trump");

        // After dealing the remaining cards, each player's hand should now have 8 cards
        for (Player player : List.of(playerA1, playerA2, playerB1, playerB2)) {
            assertEquals(8, player.getHand().size(),
                    "Player " + player.getId() + " should have 8 cards after trump is called");
        }

        // Talon should be empty after dealing remaining cards
        assertEquals(0, game.getTalon().size(), "Talon should be empty after dealing remaining cards");
    }
}