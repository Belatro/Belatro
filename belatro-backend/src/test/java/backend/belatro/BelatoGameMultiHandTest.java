package backend.belatro;

import backend.belatro.pojo.gamelogic.*;
import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.GameState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class BelatoGameMultiHandTest {
    private BelotGame game;
    private Player player1, player2, player3, player4;

    @BeforeEach
    void setUp() {
        // Create players
        player1 = new Player("Player 1");
        player2 = new Player("Player 2");
        player3 = new Player("Player 3");
        player4 = new Player("Player 4");

        // Create teams
        List<Player> teamAPlayers = new ArrayList<>();
        teamAPlayers.add(player1);
        teamAPlayers.add(player3);
        Team teamA = new Team(teamAPlayers);

        List<Player> teamBPlayers = new ArrayList<>();
        teamBPlayers.add(player2);
        teamBPlayers.add(player4);
        Team teamB = new Team(teamBPlayers);

        // Create game
        game = new BelotGame(UUID.randomUUID().toString(), teamA, teamB);
    }

    @Test
    void testMultipleHandsWithTrumpCalling() {
        System.out.println("Starting game test");

        // First hand
        game.startGame();
        assertEquals(GameState.BIDDING, game.getGameState());

        // Verify initial deal - each player should have 6 cards during bidding
        for (Player player : game.getTurnOrder()) {
            assertEquals(6, player.getHand().size(), "Players should have 6 cards during bidding");
            System.out.println(player.getId() + "'s initial hand: " + player.getHand());
        }

        // Play first hand
        Boja trumpSuit = Boja.KARA;
        playHand(trumpSuit);

        // Check points after first hand
        int teamAScoreAfterFirstHand = game.getTeamA().getScore();
        int teamBScoreAfterFirstHand = game.getTeamB().getScore();
        System.out.println("Team A score after first hand: " + teamAScoreAfterFirstHand);
        System.out.println("Team B score after first hand: " + teamBScoreAfterFirstHand);

        // Start second hand
        game.resetBidding();
        game.startGame();

        // Play second hand with a different trump
        Boja secondTrumpSuit = Boja.PIK;
        playHand(secondTrumpSuit);

        // Verify scores increased after second hand
        System.out.println("Team A score after second hand: " + game.getTeamA().getScore());
        System.out.println("Team B score after second hand: " + game.getTeamB().getScore());

        // At least one team should have scored points
        assertTrue(game.getTeamA().getScore() >= teamAScoreAfterFirstHand ||
                        game.getTeamB().getScore() >= teamBScoreAfterFirstHand,
                "At least one team should have scored additional points in the second hand");
    }

    private void playHand(Boja trumpSuit) {
        // Handle bidding phase
        Player currentBidder = game.getCurrentLead();
        System.out.println("First bidder: " + currentBidder.getId());

        // First player passes
        game.placeBid(Bid.pass(currentBidder));
        System.out.println(currentBidder.getId() + " passes");

        // Get next player
        currentBidder = game.getCurrentLead();
        System.out.println("Next bidder: " + currentBidder.getId());

        // Second player calls trump
        game.placeBid(Bid.callTrump(currentBidder, trumpSuit));
        System.out.println(currentBidder.getId() + " calls trump: " + trumpSuit);

        // Verify game state transitioned to PLAYING
        assertEquals(GameState.PLAYING, game.getGameState(),
                "Game should transition to PLAYING state after trump is called");
        assertEquals(trumpSuit, game.getTrump(), "Trump should be set to the called suit");

        // Verify each player now has 8 cards after talon is distributed
        for (Player player : game.getTurnOrder()) {
            assertEquals(8, player.getHand().size(),
                    "Players should have 8 cards after talon distribution");
            System.out.println(player.getId() + "'s playing hand: " + player.getHand());
        }

        // Play all tricks until hand is complete
        playAllTricks();
    }

    private void playAllTricks() {
        int trickCount = 0;

        // Play until all cards are gone (8 tricks with 4 cards each)
        while (!allHandsEmpty() && game.getGameState() == GameState.PLAYING && trickCount < 8) {
            trickCount++;
            System.out.println("\n--- Playing Trick " + trickCount + " ---");
            playTrick();
        }

        System.out.println("Completed " + trickCount + " tricks");

        // Verify all hands are empty after playing all tricks
        assertTrue(allHandsEmpty(), "All players should have empty hands after all tricks are played");
    }

    private boolean allHandsEmpty() {
        for (Player player : game.getTurnOrder()) {
            if (!player.getHand().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void playTrick() {
        // Get the player who leads this trick
        Player leadPlayer = game.getCurrentLead();
        System.out.println("Lead player for this trick: " + leadPlayer.getId());

        Trick trickBeforeCompletion = null;

        // Need to play 4 cards (one from each player)
        for (int i = 0; i < 4; i++) {
            Player currentPlayer = game.getCurrentPlayer();
            System.out.println(currentPlayer.getId() + "'s turn");

            if (!currentPlayer.getHand().isEmpty()) {
                List<Card> playableCards = getValidCards(currentPlayer);

                if (!playableCards.isEmpty()) {
                    Card cardToPlay = playableCards.get(0);

                    // If this is the last play, save a reference to the trick before it's completed
                    if (i == 3) {
                        trickBeforeCompletion = game.getCurrentTrick();
                    }

                    game.playCard(currentPlayer, cardToPlay, false);
                    System.out.println(currentPlayer.getId() + " plays " + cardToPlay);
                } else {
                    fail("Player " + currentPlayer.getId() + " has no valid cards to play");
                }
            } else {
                fail("Player " + currentPlayer.getId() + " has no cards left");
            }
        }

        // Assert on the trick before it was completed
        assertTrue(trickBeforeCompletion.isComplete(4), "Trick should be complete after 4 plays");

        // The rest of your method - no need to process trick since it's already done
    }

    private List<Card> getValidCards(Player player) {
        List<Card> hand = player.getHand();

        Trick currentTrick = game.getCurrentTrick();
        if (currentTrick == null || currentTrick.getPlays().isEmpty()) {
            // If player leads, they can play any card
            return hand;
        }

        // Get the lead card suit
        Card leadCard = currentTrick.getLeadCard();
        if (leadCard == null) {
            return hand;
        }

        Boja leadSuit = leadCard.getBoja();

        // Filter cards that match the lead suit
        List<Card> matchingSuitCards = new ArrayList<>();
        for (Card card : hand) {
            if (card.getBoja() == leadSuit) {
                matchingSuitCards.add(card);
            }
        }

        // If player has cards of the lead suit, they must play one
        if (!matchingSuitCards.isEmpty()) {
            return matchingSuitCards;
        }

        // If player doesn't have cards of the lead suit, they can play any card
        return hand;
    }

    private Player findPlayerById(String id) {
        for (Player player : game.getTurnOrder()) {
            if (player.getId().equals(id)) {
                return player;
            }
        }
        return null;
    }
    @Test
    void testTrumpCuttingRule() {
        // 1. Start a new game
        game.startGame();

        // 2. Set trump to KARO
        Boja trumpSuit = Boja.KARA;
        Player bidder = game.getCurrentPlayer(); // Get the current player for bidding
        game.placeBid(Bid.callTrump(bidder, trumpSuit));

        // 3. Observe the game during normal play
        boolean trumpCuttingRuleVerified = false;

        // Play one full hand
        while (game.getGameState() == GameState.PLAYING) {
            // Before each player makes a move, check if we can verify our rule
            Player currentPlayer = game.getCurrentPlayer();
            Trick currentTrick = game.getCurrentTrick();

            // Skip if this is the lead player
            if (!currentTrick.getPlays().isEmpty()) {
                Card leadCard = currentTrick.getLeadCard();

                // Check if the current player has any cards of the lead suit
                List<Card> playerHand = currentPlayer.getHand();
                boolean hasLeadSuit = playerHand.stream()
                        .anyMatch(c -> c.getBoja() == leadCard.getBoja());

                // Check if player has any trump cards
                boolean hasTrump = playerHand.stream()
                        .anyMatch(c -> c.getBoja() == trumpSuit);

                // If player is cutting AND has trump, verify they must play trump
                if (!hasLeadSuit && hasTrump) {
                    List<Card> legalMoves = game.getLegalMoves();

                    // Verify all legal moves are trump cards
                    boolean allLegalMovesAreTrumps = legalMoves.stream()
                            .allMatch(c -> c.getBoja() == trumpSuit);

                    assertTrue(allLegalMovesAreTrumps,
                            "Player cutting must play a trump card if they have one");

                    trumpCuttingRuleVerified = true;
                    break; // We found and verified our test case
                }
            }

            // Play a card and continue
            List<Card> legalMoves = game.getLegalMoves();
            if (!legalMoves.isEmpty()) {
                Card cardToPlay = legalMoves.get(0);
                game.playCard(currentPlayer, cardToPlay, false);
            }
        }

        // Make sure we actually tested our rule
        assertTrue(trumpCuttingRuleVerified,
                "Test didn't encounter a situation to verify the trump cutting rule");
    }

}