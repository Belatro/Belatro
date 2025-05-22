package backend.belatro.pojo.gamelogic;

import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.GameState;
import backend.belatro.pojo.gamelogic.enums.Rank;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * Represents a Belot card game.
 * Manages the game state, player turns, card dealing, trump calling, and scoring.
 */
public class BelotGame {
    @Getter
    private final String gameId;

    @Getter
    private final Team teamA;

    @Getter
    private final Team teamB;


    @Getter
    private final List<Player> turnOrder = new ArrayList<>();

    @JsonProperty
    private Deck deck;

    @Getter
    private List<Card> talon = new ArrayList<>();

    @Getter
    private final List<Trick> completedTricks = new ArrayList<>();

    @Getter
    private Trick currentTrick;

    @Getter
    private Player currentLead;


    private Player currentPlayer;

    @Getter
    private Player dealer;

    @Getter
    private Boja trump; // The selected trump suit

    private boolean trumpCalled = false;

    @Getter
    private GameState gameState = GameState.INITIALIZED;

    @JsonIdentityReference(alwaysAsId = true)
    private Player trumpCaller;

    @Getter
    private final List<Bid> bids = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(BelotGame.class);

    private int teamAHandPoints = 0;
    private int teamBHandPoints = 0;

    private final Map<String, Boolean> belaAlreadyDeclared = new HashMap<>();

    private int teamATricksWon = 0;
    private int teamBTricksWon = 0;

    @JsonCreator
    public BelotGame(
            @JsonProperty("gameId")
            String gameId,
            @JsonProperty("teamA")
            Team teamA,
            @JsonProperty("teamB")
            Team teamB) {
        this.gameId = Objects.requireNonNull(gameId, "Game ID cannot be null");
        this.teamA = Objects.requireNonNull(teamA, "Team A cannot be null");
        this.teamB = Objects.requireNonNull(teamB, "Team B cannot be null");

        turnOrder.addAll(teamA.getPlayers());
        turnOrder.addAll(teamB.getPlayers());
    }

    public void selectDealer() {
        Random random = new Random();
        int dealerIndex = random.nextInt(turnOrder.size());
        dealer = turnOrder.get(dealerIndex);

        List<Player> newTurnOrder = new ArrayList<>();
        for (int i = 0; i < turnOrder.size(); i++) {
            int index = (dealerIndex + 1 + i) % turnOrder.size();
            newTurnOrder.add(turnOrder.get(index));
        }
        turnOrder.clear();
        turnOrder.addAll(newTurnOrder);

        currentLead = turnOrder.getFirst();
    }

    /**
     * Starts the game by selecting a dealer, shuffling the deck, and dealing the initial cards.
     */
    public void startGame() {
        selectDealer();

        deck = new Deck();
        deck.shuffle();

        // 6 cards
        deck.dealInitialHands(turnOrder);

        if (deck.getCardsRemaining() != 8) {
            throw new IllegalStateException(
                    "Unexpected number of cards remaining: " + deck.getCardsRemaining() +
                            " (expected 8)"
            );
        }

        // 2 cards
        talon = deck.dealTalon();



        gameState = GameState.BIDDING;

        int dealerIndex = turnOrder.indexOf(dealer);
        currentLead = turnOrder.get((dealerIndex + 1) % 4);
    }

    /**
     * Processes a player's bid (pass or call trump).
     *
     * @param bid The bid to process
     * @return True if the bid was successful and processed
     */
    public boolean placeBid(Bid bid) {
        if (gameState != GameState.BIDDING) {
            throw new IllegalStateException("Bidding can only occur during bidding phase");
        }

        Player player = bid.getPlayer();

        // Check if it's this player's turn to bid
        if (!player.getId().equals(currentLead.getId())) {
            return false;
        }

        // Handle passing
        if (bid.isPass()) {
            return handlePass(player);
        }

        // Handle calling trump
        if (bid.isTrumpCall()) {
            return handleTrumpCall(player, bid.getSelectedTrump());
        }

        return false;
    }

    /**
     * Handles a player's decision to pass during bidding.
     */
    private boolean handlePass(Player player) {
        // If dealer is passing and must call trump, don't allow passing
        if (player == dealer && isDealerForcedToCallTrump()) {
            return false;
        }

        // Record that this player passed
        player.setBidPassed(true);

        // Add to bid history
        Bid passBid = Bid.pass(player);
        bids.add(passBid);
        player.recordBid(passBid);

        // Move to the next player
        moveToNextBidder();

        return true;
    }

    /**
     * Handles a player calling trump during bidding.
     */
    private boolean handleTrumpCall(Player player, Boja selectedTrump) {
        // Create and record the bid
        Bid trumpBid = Bid.callTrump(player, selectedTrump);
        bids.add(trumpBid);
        player.recordBid(trumpBid);

        // Set the trump suit
        trump = selectedTrump;
        trumpCalled = true;
        trumpCaller = player;

        // Deal the remaining cards
        dealRemainingCards();

        // Transition to the playing phase
        gameState = GameState.PLAYING;

        // Set the current lead for the first trick
        setupFirstTrick();

        // Process any declarations (belot, sequences, etc.)
        processDeclarations();

        return true;
    }

    /**
     * Checks if all players except the dealer have passed during bidding.
     */
    private boolean isDealerForcedToCallTrump() {
        if (currentLead != dealer) {
            return false;
        }

        for (Player player : turnOrder) {
            if (player != dealer && !player.hasBidPassed()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Moves the bidding to the next player in turn order.
     */
    private void moveToNextBidder() {
        int currentIndex = turnOrder.indexOf(currentLead);
        currentLead = turnOrder.get((currentIndex + 1) % turnOrder.size());
    }

    /**
     * Forces the dealer to call trump. This is used when all other players have passed.
     */
    public boolean dealerCallTrump(Boja selectedTrump) {
        if (gameState != GameState.BIDDING || currentLead != dealer || !isDealerForcedToCallTrump()) {
            return false;
        }

        return handleTrumpCall(dealer, selectedTrump);
    }

    /**
     * Sets up the first trick after bidding is complete.
     */
    private void setupFirstTrick() {
        // In many Belot variants, the first lead is given to the player after the dealer
        int dealerIndex = turnOrder.indexOf(dealer);
        int leadIndex = (dealerIndex + 1) % turnOrder.size();

        // Set the current lead player
        currentLead = turnOrder.get(leadIndex);

        // IMPORTANT: Initialize the currentPlayer field to be the same as the lead player
        currentPlayer = currentLead;
        teamATricksWon = 0;
        teamBTricksWon = 0;

        // Initialize the currentTrick with the trump suit
        // Note: The Trick constructor in your code takes leadPlayerId and trump,
        // but we should adapt based on your Trick implementation
        currentTrick = new Trick(currentLead.getId(), trump);

        // Set the game state to PLAYING
        gameState = GameState.PLAYING;

        resetBelaDeclarations();
    }


    /**
     * Resets the bidding state for a new hand.
     */
    public void resetBidding() {
        bids.clear();
        trumpCalled = false;
        trumpCaller = null;
        trump = null;

        for (Player player : turnOrder) {
            player.resetBidding();
        }
    }


    /**
     * Deals the remaining two cards per player (6 → 8) after trump is chosen.
     * Uses the *real* deck and then clears the talon list.
     */
    private void dealRemainingCards() {

        // put the two talon cards back on top so they’re dealt first
        deck.cards.addAll(0, talon);   // package-private field → same class
        talon.clear();

        // give everyone cards until he/she holds eight
        for (Player p : turnOrder) {
            p.getHand().addAll(deck.deal(2));
            LOGGER.warn("{} now holds {}", p.getId(), p.getHand().size());
        }
        LOGGER.warn("### entering dealRemainingCards() ###");
    }



    /**
     * Process all declarations from players and determine which ones count.
     */
    private void processDeclarations() {
        // Store declarations by player: player -> (type -> points)
        Map<Player, Map<String, Integer>> allDeclarations = new HashMap<>();

        // First, find all declarations
        for (Player player : turnOrder) {
            Map<String, Integer> playerDeclarations = new HashMap<>();

            // Check sequences in all suits
            Map<Boja, Integer> sequences = ZvanjaValidator.evaluateAllSequences(player.getHand());
            if (!sequences.isEmpty()) {
                // Find the highest sequence
                int highestSequence = sequences.values().stream()
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(0);

                if (highestSequence > 0) {
                    playerDeclarations.put("sequence", highestSequence);
                }
            }

            // Check four-of-a-kind
            ZvanjaValidator.evaluateFourOfAKind(player.getHand())
                    .ifPresent(points -> playerDeclarations.put("fourOfAKind", points));

            if (!playerDeclarations.isEmpty()) {
                allDeclarations.put(player, playerDeclarations);
            }
        }

        // Process each type of declaration
        processDeclarationType(allDeclarations, "sequence");
        processDeclarationType(allDeclarations, "fourOfAKind");


    }

    /**
     * Process a specific type of declaration according to Belot rules.
     * If both teams have the same highest value, the player earlier in turn order wins.
     *
     * @param allDeclarations Map of all declarations from all players
     * @param declarationType The type of declaration to process (e.g., "sequence", "fourOfAKind")
     */
    private void processDeclarationType(Map<Player, Map<String, Integer>> allDeclarations, String declarationType) {
        // Extract declarations of this type
        Map<Player, Integer> declarations = new HashMap<>();
        for (Map.Entry<Player, Map<String, Integer>> entry : allDeclarations.entrySet()) {
            if (entry.getValue().containsKey(declarationType)) {
                declarations.put(entry.getKey(), entry.getValue().get(declarationType));
            }
        }

        if (declarations.isEmpty()) {
            return;
        }

        // Find the highest value
        int highestValue = declarations.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        // Find players with the highest value
        List<Player> playersWithHighestValue = declarations.entrySet().stream()
                .filter(e -> e.getValue() == highestValue)
                .map(Map.Entry::getKey)
                .toList();

        // If there's a tie, position in turn order decides
        Player winningPlayer;
        if (playersWithHighestValue.size() > 1) {
            // Find the first player in turn order among those with highest value
            winningPlayer = playersWithHighestValue.stream()
                    .min(Comparator.comparing(turnOrder::indexOf))
                    .orElse(null);
        } else {
            winningPlayer = playersWithHighestValue.get(0);
        }

        if (winningPlayer == null) {
            LOGGER.warn("No winning player found for " + declarationType + " declarations");
            return;
        }

        // Award points to the winning team
        Team winningTeam = teamA.getPlayers().contains(winningPlayer) ? teamA : teamB;

        // Get all valid declarations from the winning team
        for (Player player : winningTeam.getPlayers()) {
            if (declarations.containsKey(player)) {
                int points = declarations.get(player);
                if (winningTeam == teamA) {
                    teamAHandPoints += points;
                    LOGGER.info("Added " + points + " declaration points to Team A hand points, now: " + teamAHandPoints);
                } else {
                    teamBHandPoints += points;
                    LOGGER.info("Added " + points + " declaration points to Team B hand points, now: " + teamBHandPoints);
                }

                LOGGER.info("Player " + player.getId() + " " + declarationType +
                        " declaration of " + points + " points awarded to team");


            }
        }
    }
    private void advanceToNextPlayer(Player player) {
        int currentIndex = turnOrder.indexOf(player);
        int nextIndex = (currentIndex + 1) % turnOrder.size();
        this.currentPlayer = turnOrder.get(nextIndex);
    }






    /**
     * Handles a player declaring Bela when playing a card.
     * Bela is valid when a player has both King and Queen of the trump suit.
     *
     * @param playerId The ID of the player declaring Bela
     * @return True if the declaration was valid and points were awarded
     */
    public boolean declareBela(String playerId) {
        // Get player object from ID
        Player player = findPlayerById(playerId);
        if (player == null) {
            LOGGER.warn("Player not found for ID: " + playerId);
            return false;
        }

        // Check if the game is in playing state
        if (gameState != GameState.PLAYING) {
            LOGGER.warn("Attempted to declare Bela while game is not in playing state");
            return false;
        }

        // Check if player has already declared Bela this hand
        if (belaAlreadyDeclared.getOrDefault(playerId, false)) {
            LOGGER.warn("Player " + playerId + " already declared Bela in this hand");
            return false;
        }

        // Verify the player actually has a valid Bela (King+Queen of trump)
        List<Card> playerHand = player.getHand();
        boolean hasKing = playerHand.stream().anyMatch(card ->
                card.getBoja() == trump && card.getRank() == Rank.KRALJ);
        boolean hasQueen = playerHand.stream().anyMatch(card ->
                card.getBoja() == trump && card.getRank() == Rank.BABA);

        if (!hasKing || !hasQueen) {
            LOGGER.warn("Player " + playerId + " tried to declare Bela but doesn't have King and Queen of trump");
            return false;
        }

        // Award points to the player's team
        Team team = getPlayerTeam(player);
        if (team != null) {
            if (team == teamA) {
                teamAHandPoints += 20;
                LOGGER.info("Added 20 bela points to Team A hand points, now: " + teamAHandPoints);
            } else {
                teamBHandPoints += 20;
                LOGGER.info("Added 20 bela points to Team B hand points, now: " + teamBHandPoints);
            }

            LOGGER.info("Player " + playerId + " declared Bela for 20 points");

        }

        // Mark player as having declared Bela for this hand
        belaAlreadyDeclared.put(playerId, true);
        return true;
    }
    /**
     * Determines which team a player belongs to.
     */
    private Team getPlayerTeam(Player player) {
        if (teamA.getPlayers().contains(player)) {
            return teamA;
        } else if (teamB.getPlayers().contains(player)) {
            return teamB;
        }
        return null;
    }

    /**
     * Finds a player by their ID.
     */
    public  Player findPlayerById(String id) {
        return turnOrder.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .orElse(null);
    }






    /**
     * Plays a card on behalf of the given player.
     * If declareBela is true, validates and processes a Bela declaration.
     *
     * @param player the player making the move
     * @param card the card to play
     * @param declareBela whether the player is attempting a Bela declaration
     * @return true if the move is processed successfully, false otherwise
     */
    public boolean playCard(Player player, Card card, boolean declareBela) {
        if (player == null || card == null) {
            throw new NullPointerException("Player or card cannot be null");
        }

        // Process Bela declaration if requested.
        if (declareBela) {
            boolean declared = processBela(player, card);
            if (!declared) {
                // Optionally, log the failed declaration or notify the user.
                System.out.println("Invalid Bela declaration attempted by player: " + player.getId());
            }
        }

        // Let the player remove the card from their hand.
        Card playedCard = player.playCard(card);

        // Add the played card to the current trick.
        currentTrick.addPlay(player.getId(), playedCard);

        if (!currentTrick.isComplete(turnOrder.size())) {
            advanceToNextPlayer(player);
        }

        // Check if the trick is complete (based on turn order size).
        if (currentTrick.isComplete(turnOrder.size())) {
            completeTrick();
        }

        return true;
    }

    /**
     * Processes a Bela declaration for the given player and card.
     * Validates that the card is either BABA or KRALJ of trump and that the matching card
     * is present in the player's hand.
     *
     * @param player the player attempting the declaration
     * @param card the card being declared (BABA or KRALJ of trump)
     * @return true if the declaration is valid and processed; false otherwise
     */
    private boolean processBela(Player player, Card card) {
        if (belaAlreadyDeclared.getOrDefault(player.getId(), false)) {
            // Player has already declared Bela in this hand.
            return false;
        }

        boolean isBabaOfTrump = card.getBoja() == trump && card.getRank() == Rank.BABA;
        boolean isKraljOfTrump = card.getBoja() == trump && card.getRank() == Rank.KRALJ;

        // Declaration is only allowed if playing BABA or KRALJ of trump.
        if (!isBabaOfTrump && !isKraljOfTrump) {
            return false;
        }

        // Check whether the matching card exists in the player's hand.
        boolean hasMatchingCard = player.getHand().stream().anyMatch(c ->
                c.getBoja() == trump &&
                        ((isBabaOfTrump && c.getRank() == Rank.KRALJ) ||
                                (isKraljOfTrump && c.getRank() == Rank.BABA))
        );

        if (!hasMatchingCard) {
            return false;
        }

        // Award bonus points (usually 20 points) to the player's team.
        Team playerTeam = getPlayerTeam(player);
        if (playerTeam != null) {
            if (playerTeam == teamA) {
                teamAHandPoints += 20;
                LOGGER.info("Added 20 bela points to Team A hand points, now: " + teamAHandPoints);
            } else {
                teamBHandPoints += 20;
                LOGGER.info("Added 20 bela points to Team B hand points, now: " + teamBHandPoints);
            }

            LOGGER.info("Player " + player.getId() + " declared Bela for 20 points");

        }

        // Mark this player as having declared Bela.
        belaAlreadyDeclared.put(player.getId(), true);
        return true;
    }




    /**
     * Resets all Bela declarations for a new hand.
     */
    private void resetBelaDeclarations() {
        belaAlreadyDeclared.clear();
    }


    /**
     * @param player The player playing the card
     * @param card The card being played
     * @return true if the play is valid, false otherwise
     */
    private boolean isValidPlay(Player player, Card card) {
        if (currentTrick.getPlays().isEmpty()) {
            return true;
        }

        Card leadCard = currentTrick.getLeadCard();
        Boja leadSuit = leadCard.getBoja();

        if (card.getBoja() == leadSuit) {
            return true;
        }

        boolean hasLeadSuit = player.getHand().stream()
                .anyMatch(c -> c.getBoja() == leadSuit);

        if (!hasLeadSuit) {
            boolean trumpInTrick = currentTrick.getPlays().values().stream()
                    .anyMatch(c -> c.getBoja() == trump);

            if (trumpInTrick && card.getBoja() != trump) {
                boolean hasTrump = player.getHand().stream()
                        .anyMatch(c -> c.getBoja() == trump);

                return !hasTrump;
            }

            return true;
        }

        return false;
    }

    /**
     * @param player The player to check
     * @return true if it's the player's turn, false otherwise
     */
    private boolean isPlayerTurn(Player player) {
        if (currentTrick.getPlays().isEmpty()) {
            return player.equals(currentLead);
        }

        for (Player p : turnOrder) {
            if (!currentTrick.getPlays().containsKey(p.getId())) {
                return player.equals(p);
            }
        }

        return false;
    }

    /**
     * @param trickWinner The player who won the trick
     * @param trickPoints The points earned from the trick
     */
    private void updateScores(Player trickWinner, int trickPoints) {
        if (teamA.getPlayers().contains(trickWinner)) {
            teamA.addPoints(trickPoints);
        } else if (teamB.getPlayers().contains(trickWinner)) {
            teamB.addPoints(trickPoints);
        }
    }




    /**
     * @return Team A's score
     */
    public int getTeamAScore() {
        return teamA.getScore();
    }

    /**
     * @return Team B's score
     */
    public int getTeamBScore() {
        return teamB.getScore();
    }

    /**
     * @return The winning team, or null if no winner yet or tie
     */
    public Team getWinner() {
        if (gameState != GameState.COMPLETED) {
            return null; // Game is not over yet
        }

        if (teamA.getScore() > teamB.getScore()) {
            return teamA;
        } else if (teamB.getScore() > teamA.getScore()) {
            return teamB;
        } else {
            return null; // It's a tie
        }
    }

    /**
     * Apply the "padanje" rule and update the total scores.
     * This should be called after all tricks have been played and before moving to the next hand.
     */
    private void applyPadanjeAndUpdateScores() {
        // No padanje if no one called trump
        if (trumpCaller == null) {
            // Just add hand points to total score
            teamA.addPoints(teamAHandPoints);
            teamB.addPoints(teamBHandPoints);
            return;
        }

        // Determine which team called trump
        Team trumpCallerTeam = null;
        Team opposingTeam = null;

        if (teamA.getPlayers().contains(trumpCaller)) {
            trumpCallerTeam = teamA;
            opposingTeam = teamB;
        } else {
            trumpCallerTeam = teamB;
            opposingTeam = teamA;
        }

        // Get hand points for each team
        int trumpCallerHandPoints = (trumpCallerTeam == teamA) ? teamAHandPoints : teamBHandPoints;
        int opposingTeamHandPoints = (opposingTeam == teamA) ? teamAHandPoints : teamBHandPoints;

        LOGGER.info("Hand complete. Trump caller team: {} points, Opposing team: {} points",
                trumpCallerHandPoints, opposingTeamHandPoints);

        // Check if the trump caller team "falls"
        if (trumpCallerHandPoints <= opposingTeamHandPoints) {
            LOGGER.info("Trump caller team falls! Their points are transferred to the opposing team.");

            if (trumpCallerTeam == teamA) {
                // Team A falls, add their points to Team B
                teamB.addPoints(teamAHandPoints + teamBHandPoints);
            } else {
                // Team B falls, add their points to Team A
                teamA.addPoints(teamAHandPoints + teamBHandPoints);
            }
        } else {
            // No falling, add hand points normally
            teamA.addPoints(teamAHandPoints);
            teamB.addPoints(teamBHandPoints);
        }

        // Reset hand points for the next hand
        teamAHandPoints = 0;
        teamBHandPoints = 0;

        LOGGER.info("After applying padanje rule - Team A total: {}, Team B total: {}",
                teamA.getScore(), teamB.getScore());
    }

    /**
     * Completes the current trick, determines the winner, and sets up the next trick.
     */
    private void completeTrick() {
        String winnerPlayerId = currentTrick.determineWinner();
        Player winner = findPlayerById(winnerPlayerId);
        if (winner != null) {
            if (teamA.getPlayers().contains(winner)) {
                               teamATricksWon++;
                           } else {
                               teamBTricksWon++;
                           }
            // Award trick points to winner's team for the current hand
            Team winningTeam = getPlayerTeam(winner);
            int trickPoints = currentTrick.calculatePoints();

            // Add more detailed logging
            LOGGER.info("Trick completed. Winner ID: {}, Points: {}",
                    winner.getId(), trickPoints);

            // Record the completed trick
            completedTricks.add(currentTrick);

            // Check if this is the last trick (but don't apply padanje yet)
            boolean isLastTrick = completedTricks.size() == 8;

            if (isLastTrick) {
                // Add last trick bonus (10 points)
                trickPoints += 10;
                LOGGER.info("Last trick bonus (10 points) awarded to {}",
                        winningTeam == teamA ? "Team A" : "Team B");
            }

            // Add points to the appropriate team
            if (winningTeam == teamA) {
                teamAHandPoints += trickPoints;
                LOGGER.info("Team A now has {} hand points", teamAHandPoints);
            } else if (winningTeam == teamB) {
                teamBHandPoints += trickPoints;
                LOGGER.info("Team B now has {} hand points", teamBHandPoints);
            }

            // If it was the last trick, apply padanje rule AFTER all points are counted
            if (isLastTrick) {
                LOGGER.info("Final hand points - Team A: {}, Team B: {}, Total: {}",
                        teamAHandPoints, teamBHandPoints, (teamAHandPoints + teamBHandPoints));

                // Apply padanje rule and update scores after all points are counted

                LOGGER.info("Before applying padanje - Team A: {} hand points, {} total score, Team B: {} hand points, {} total score",
                        teamAHandPoints, teamA.getScore(), teamBHandPoints, teamB.getScore());

                applyPadanjeAndUpdateScores();
                if (teamATricksWon == 8) {
                    teamAHandPoints += 90;
                    LOGGER.info("Team A won all tricks! Capot bonus of 90 awarded.");
                } else if (teamBTricksWon == 8) {
                    teamBHandPoints += 90;
                    LOGGER.info("Team B won all tricks! Capot bonus of 90 awarded.");
                }
                // Set game state to hand complete
                trumpCaller = null;
                trumpCalled = false;
                trump = null;
                gameState = GameState.HAND_COMPLETE;

                return;
            }

            // The winner leads the next trick
            currentLead = winner;
            currentTrick = new Trick(winner.getId(), trump);
        }
    }
    /**
     * Processes the current trick, determines the winner, and sets up the next trick.
     * This is a public interface to the private completeTrick functionality.
     *
     * @return The player who won the trick
     */
    public Player processTrick() {
        String winnerPlayerId = currentTrick.determineWinner();
        Player winner = findPlayerById(winnerPlayerId);

        completeTrick();

        return winner;
    }


    /**
     * Checks if the hand is complete (all tricks played).
     * @return True if hand is complete, false otherwise
     */
    public boolean checkHandComplete() {
        // In Belot, each player gets 8 cards, so there are 8 tricks total
        return completedTricks.size() == 8;
    }


    public Player getCurrentPlayer() {
        if (gameState == GameState.BIDDING) {
            return currentLead;
        }

        if (gameState == GameState.PLAYING) {
            if (currentTrick.getPlays().isEmpty()) {
                return currentLead;
            }

            for (Player p : turnOrder) {
                if (!currentTrick.getPlays().containsKey(p.getId())) {
                    return p;
                }
            }
        }

        return null;
    }

    /**
     * Adds points earned from a trick to the winning team's hand points
     */
    private void addTrickPoints(Player trickWinner, int points) {
        if (teamA.getPlayers().contains(trickWinner)) {
            teamAHandPoints += points;
            LOGGER.debug("Team A earned {} points from trick, now has {} hand points",
                    points, teamAHandPoints);
        } else if (teamB.getPlayers().contains(trickWinner)) {
            teamBHandPoints += points;
            LOGGER.debug("Team B earned {} points from trick, now has {} hand points",
                    points, teamBHandPoints);
        }
    }


    /**
     * @return The number of completed tricks
     */
    public int getCompletedTrickCount() {
        return completedTricks.size();
    }



    /**
     * @return A map of player IDs to cards played in the current trick
     */
    @JsonIgnore
    public Map<String, Card> getCurrentTrickPlays() {
        return currentTrick != null ? currentTrick.getPlays() : Collections.emptyMap();
    }
    @JsonIgnore
    public List<Card> getLegalMoves() {
        Player currentPlayer = getCurrentPlayer();
        if (currentPlayer == null) {
            return Collections.emptyList();
        }

        List<Card> hand = currentPlayer.getHand();

        // 1) If nobody has led yet, you can play anything
        if (currentTrick.getPlays().isEmpty()) {
            return new ArrayList<>(hand);
        }

        // 2) There is a lead card — pull it and its suit
        Card leadCard = currentTrick.getLeadCard();
        Boja leadSuit = leadCard.getBoja();

        // 3) If you have cards of the lead suit…
        List<Card> leadSuitCards = hand.stream()
                .filter(c -> c.getBoja() == leadSuit)
                .toList();
        if (!leadSuitCards.isEmpty()) {
            // 3a) Find the card you must beat
            Card currentWinner = currentTrick.getWinningCard();
            // 3b) Choose the right ordering (trump order if leadSuit is trump, else normal)
            Comparator<Card> comparator = (leadSuit == trump)
                    ? BelotRankComparator.getTrumpComparator()
                    : BelotRankComparator.getNonTrumpComparator();
            // 3c) Of your lead-suit cards, see which actually outrank the current winner
            List<Card> overcards = leadSuitCards.stream()
                    .filter(c -> comparator.compare(c, currentWinner) > 0)
                    .toList();
            // 3d) If you have any overcards, you must play one; otherwise you must still follow suit
            return overcards.isEmpty() ? leadSuitCards : overcards;
        }

        // 4) You have no lead suit — if you have any trump cards, you must play one
        List<Card> trumpCards = hand.stream()
                .filter(c -> c.getBoja() == trump)
                .toList();
        if (!trumpCards.isEmpty()) {
            // 4a) Check if someone already trumped; if so, find the highest trump in the trick
            Optional<Card> highestTrumpInTrick = currentTrick.getPlays().values().stream()
                    .filter(c -> c.getBoja() == trump)
                    .max(BelotRankComparator.getTrumpComparator());
            if (highestTrumpInTrick.isPresent()) {
                Card highestTrump = highestTrumpInTrick.get();
                // 4b) Of your trumps, see which outrank that
                List<Card> overtrumps = trumpCards.stream()
                        .filter(c -> BelotRankComparator.getTrumpComparator()
                                .compare(c, highestTrump) > 0)
                        .toList();
                // 4c) If you have any overtrumps, you must play one; otherwise you must still trump
                return overtrumps.isEmpty() ? trumpCards : overtrumps;
            }
            // 4d) No trump in the trick yet, but you have trumps → must play any trump
            return trumpCards;
        }

        // 5) You can neither follow suit nor trump → play anything
        return new ArrayList<>(hand);
    }

    /**
     * @param player The player to check
     * @return true if the player has any declarations, false otherwise
     */
    public boolean hasDeclarations(Player player) {

        boolean hasBela = ZvanjaValidator.isBela(player.getHand(), trump);

        Optional<Integer> sequencePoints = ZvanjaValidator.evaluateSequence(player.getHand(), trump);

        return hasBela || sequencePoints.isPresent();
    }



    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BelotGame [id=").append(gameId)
                .append(", state=").append(gameState)
                .append(", teamA=").append(teamA.getScore())
                .append(", teamB=").append(teamB.getScore())
                .append(", trump=").append(trump)
                .append(", completedTricks=").append(completedTricks.size())
                .append("]");
        return sb.toString();
    }

}
