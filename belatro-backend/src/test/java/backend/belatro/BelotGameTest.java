package backend.belatro;

import backend.belatro.pojo.gamelogic.BelotGame;
import backend.belatro.pojo.gamelogic.Player;
import backend.belatro.pojo.gamelogic.Team;
import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;


public class BelotGameTest {

    private BelotGame game;
    private Team teamA;
    private Team teamB;
    private Player player1;
    private Player player2;
    private Player player3;
    private Player player4;

    @BeforeEach
    void setUp() {
        // Create players
        player1 = new Player("player1");
        player2 = new Player("player2");
        player3 = new Player("player3");
        player4 = new Player("player4");

        // Create teams
        teamA = new Team(Arrays.asList(player1, player3));
        teamB = new Team(Arrays.asList(player2, player4));

        // Create game
        game = new BelotGame("game1", teamA, teamB);
    }

    @Test
    void testGameInitialization() {
        // Verify initial state
        assertEquals("game1", game.getGameId());
        assertEquals(teamA, game.getTeamA());
        assertEquals(teamB, game.getTeamB());
        assertEquals(GameState.INITIALIZED, game.getGameState());
        assertEquals(4, game.getTurnOrder().size());
        assertTrue(game.getCompletedTricks().isEmpty());
    }

    @Test
    void testSelectDealer() {
        game.selectDealer();

        // Verify dealer was selected
        assertNotNull(game.getDealer());

        // Verify current lead is set (player after dealer)
        assertNotNull(game.getCurrentLead());

        // Verify turn order is rearranged to start with player after dealer
        assertEquals(game.getCurrentLead(), game.getTurnOrder().get(0));
    }

    @Test
    void testStartGame() {
        // Start the game
        game.startGame();

        // Verify state changes
        assertEquals(GameState.TRUMP_CALLING, game.getGameState());
        assertNotNull(game.getDealer());
        assertNotNull(game.getCurrentLead());
        assertEquals(2, game.getTalon().size());

        // Verify each player has 6 cards
        for (Player player : game.getTurnOrder()) {
            assertEquals(6, player.getHand().size());
        }
    }

    @Test
    void testCallTrumpInWrongState() {
        // Game is in INITIALIZED state
        assertThrows(IllegalStateException.class, () -> {
            game.callTrump(player1, Boja.HERC);
        });
    }

    @Test
    void testGetGameStateDescription() {
        assertEquals("Game initialized", game.getGameStateDescription());

        // Change state and verify description updates
        game.startGame();
        assertEquals("Waiting for trump to be called", game.getGameStateDescription());
    }
}
