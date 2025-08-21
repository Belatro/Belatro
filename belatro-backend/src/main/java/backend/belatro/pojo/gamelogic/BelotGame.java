package backend.belatro.pojo.gamelogic;

import backend.belatro.callbacks.HandCompletionCallback;
import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.GameState;
import backend.belatro.pojo.gamelogic.enums.Rank;
import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * Represents a Belot card game.
 * Manages the game state, player turns, card dealing, trump calling, and scoring.
 */
public class BelotGame {
    private static final int TARGET_SCORE = 1001;
    @Getter
    private final String gameId;

    @Getter
    private final Team teamA;

    @Setter
    @JsonIgnore
    private HandCompletionCallback handCompletionCallback;

    @Getter
    private final Team teamB;

    private static final int FULL_HAND_POINTS = 162;
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
    @JsonProperty("trumpCaller")
    private Player trumpCaller;

    @Getter
    private final List<Bid> bids = new ArrayList<>();

    @Getter
    @JsonProperty private int teamADeclPoints = 0;
    @Getter
    @JsonProperty private int teamBDeclPoints = 0;

    private static final Logger LOGGER = LoggerFactory.getLogger(BelotGame.class);

    @JsonProperty
    private int teamAHandPoints = 0;
    @JsonProperty
    private int teamBHandPoints = 0;
    @JsonIdentityReference(alwaysAsId = true)
    @JsonProperty
    private final Set<Team> foulingTeams = new HashSet<>();

    /* helper methods – never persisted */
    @JsonIgnore public boolean anyFoul()         { return !foulingTeams.isEmpty(); }
    @JsonIgnore public boolean bothTeamsFouled() { return foulingTeams.size() > 1; }

    /* challenge quotas */
    @JsonProperty
    private final Map<String, Boolean> challengeUsed = new HashMap<>();

    @JsonProperty
    private final Map<String, Boolean> belaAlreadyDeclared = new HashMap<>();

    @JsonProperty
    private int teamATricksWon = 0;
    @JsonProperty
    private int teamBTricksWon = 0;
    @JsonProperty private Instant lastActivity = Instant.now();
    @JsonIgnore  public Instant getLastActivity() { return lastActivity; }

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


        for (int i = 0; i < teamA.getPlayers().size(); i++) {
            turnOrder.add(teamA.getPlayers().get(i));
            turnOrder.add(teamB.getPlayers().get(i));
        }
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

        logTurnOrder("Dealer chosen (" + dealer.getId() + ")");

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
        if (player.equals(dealer) && !trumpCalled) {
            throw new IllegalStateException(
                    "Dealer must choose a trump suit – passing is not allowed.");
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
        trumpCaller = findPlayerById(player.getId());

        // Deal the remaining cards
        dealRemainingCards();

        // Transition to the playing phase
        gameState = GameState.PLAYING;

        // Set the current lead for the first trick
        setupFirstTrick();

        // Process any declarations (belot, sequences, etc.)
        processDeclarations();

        logTurnOrder("Trump called (" + selectedTrump + ") by " + player.getId());


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
    private void resetHandState() {
        trumpCaller     = null;
        trumpCalled     = false;
        trump           = null;

        teamATricksWon  = 0;
        teamBTricksWon  = 0;
        teamAHandPoints = 0;
        teamBHandPoints = 0;
        teamADeclPoints  = 0;
        teamBDeclPoints  = 0;

        completedTricks.clear();
        belaAlreadyDeclared.clear();
        foulingTeams.clear();
        challengeUsed.clear();
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
                    teamADeclPoints += points;
                    LOGGER.info("Added " + points + " declaration points to Team A hand points, now: " + teamAHandPoints);
                } else {
                    teamBHandPoints += points;
                    teamBDeclPoints += points;
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
//    public boolean declareBela(String playerId) {
//        // Get player object from ID
//        Player player = findPlayerById(playerId);
//        if (player == null) {
//            LOGGER.warn("Player not found for ID: " + playerId);
//            return false;
//        }
//
//        // Check if the game is in playing state
//        if (gameState != GameState.PLAYING) {
//            LOGGER.warn("Attempted to declare Bela while game is not in playing state");
//            return false;
//        }
//
//        // Check if player has already declared Bela this hand
//        if (belaAlreadyDeclared.getOrDefault(playerId, false)) {
//            LOGGER.warn("Player " + playerId + " already declared Bela in this hand");
//            return false;
//        }
//
//        // Verify the player actually has a valid Bela (King+Queen of trump)
//        List<Card> playerHand = player.getHand();
//        boolean hasKing = playerHand.stream().anyMatch(card ->
//                card.getBoja() == trump && card.getRank() == Rank.KRALJ);
//        boolean hasQueen = playerHand.stream().anyMatch(card ->
//                card.getBoja() == trump && card.getRank() == Rank.BABA);
//
//        if (!hasKing || !hasQueen) {
//            LOGGER.warn("Player " + playerId + " tried to declare Bela but doesn't have King and Queen of trump");
//            return false;
//        }
//
//        // Award points to the player's team
//        Team team = getPlayerTeam(player);
//        if (team != null) {
//            if (team == teamA) {
//                teamAHandPoints += 20;
//                LOGGER.info("Added 20 bela points to Team A hand points, now: " + teamAHandPoints);
//            } else {
//                teamBHandPoints += 20;
//                LOGGER.info("Added 20 bela points to Team B hand points, now: " + teamBHandPoints);
//            }
//
//            LOGGER.info("Player " + playerId + " declared Bela for 20 points");
//
//        }
//
//        // Mark player as having declared Bela for this hand
//        belaAlreadyDeclared.put(playerId, true);
//        return true;
//    }
    /**
     * Determines which team a player belongs to.
     */
    private Team getPlayerTeam(Player player) {
        if (teamA.getPlayers().stream().anyMatch(p -> p.getId().equals(player.getId())))
            return teamA;
        if (teamB.getPlayers().stream().anyMatch(p -> p.getId().equals(player.getId())))
            return teamB;
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

        if (!isValidPlay(player, card)) {
            foulingTeams.add(getPlayerTeam(player));     // record the foul once
            LOGGER.warn("Illegal card detected – {} by {}",
                    card,
                    getPlayerTeam(player) == teamA ? "Team A" : "Team B");
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
                teamADeclPoints += 20;
                LOGGER.info("Added 20 bela points to Team A hand points, now: " + teamAHandPoints);
            } else {
                teamBHandPoints += 20;
                teamBDeclPoints += 20;
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
     * @param player The player trying to play the card
     * @param card   The card the player wants to play
     * @return true  if the card is legal under Belot rules
     *         false otherwise
     */
    public boolean isValidPlay(Player player, Card card) {
        // 1) only the active player may act
        if (!player.equals(getCurrentPlayer())) {
            return false;
        }

        // 2) let the existing rules engine decide
        return getLegalMoves().contains(card);
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
     * Apply “padanje” (falling): compare the TOTAL hand points of the caller’s
     * team against the opponents. If the caller’s team scores *less than or
     * equal to* the opponents, they fall and ALL points (caller + opponent)
     * go to the opponents; otherwise each team keeps its own.
     * Hand-point caches are always cleared before return.
     */
    private void applyPadanjeAndUpdateScores() {



        /* Determine which side the caller belongs to by player ID */
        boolean callerIsA = teamA.getPlayers()
                .stream()
                .anyMatch(p -> p.getId().equals(trumpCaller.getId()));

        int callerPts    = callerIsA ? teamAHandPoints : teamBHandPoints;
        int opponentPts  = callerIsA ? teamBHandPoints : teamAHandPoints;
        int allHandPts   = teamAHandPoints + teamBHandPoints;

        if (callerPts <= opponentPts) {        // caller FALLS
            if (callerIsA) {
                teamB.addPoints(allHandPts);   // Team B scoops everything
            } else {
                teamA.addPoints(allHandPts);   // Team A scoops everything
            }
        } else {                               // caller SUCCEEDS
            teamA.addPoints(teamAHandPoints);
            teamB.addPoints(teamBHandPoints);
        }

        /* Clear for the next hand */
        teamAHandPoints = 0;
        teamBHandPoints = 0;
    }



    /**
     * Completes the current trick, scores it, and sets up the next trick (or hand).
     * – Winner and points are taken from *currentTrick* BEFORE we rotate turnOrder.
     * – Only after scoring do we rotate the list so the winner leads the next trick.
     */
    private void completeTrick() {

        /* --- 1. who won, how many points? ----------------------------------- */
        String winnerId = currentTrick.determineWinner();
        Player winner   = findPlayerById(winnerId);
        int    trickPts = currentTrick.calculatePoints();          // << BEFORE reset

        Team  winnerTeam = getPlayerTeam(winner);
        boolean isLastTrick = (completedTricks.size() == 7);       // 7 → this is #8

        if (isLastTrick) {
            trickPts += 10;                                        // last-trick bonus
            LOGGER.info("Last trick bonus (+10) to {}", winnerId);
        }

        /* update per-hand tallies */
        if (winnerTeam == teamA) {
            teamATricksWon++;
            LOGGER.info("DEBUG: teamATricksWon now = " + teamATricksWon);

            teamAHandPoints += trickPts;
        } else {
            teamBTricksWon++;
            LOGGER.info("DEBUG: teamBTricksWon now = " + teamBTricksWon);

            teamBHandPoints += trickPts;
        }
        LOGGER.info("Trick completed. Winner={}, Points={}, HandPts A={}, B={}",
                winnerId, trickPts, teamAHandPoints, teamBHandPoints);

        completedTricks.add(currentTrick);

        /* --- 2. hand finished? ---------------------------------------------- */
        if (isLastTrick) {
            LOGGER.info("Hand totals before padanje – A:{}  B:{}",
                    teamAHandPoints, teamBHandPoints);
            if (teamATricksWon == 0 && teamAHandPoints > 0) {
                LOGGER.info("Team A had declarations but won no tricks; clearing their hand points");
                teamAHandPoints = 0;
            }
            if (teamBTricksWon == 0 && teamBHandPoints > 0) {
                LOGGER.info("Team B had declarations but won no tricks; clearing their hand points");
                teamBHandPoints = 0;
            }

            int finalTeamAHandPoints = teamAHandPoints;
            int finalTeamBHandPoints = teamBHandPoints;
            int finalTeamATricksWon  = teamATricksWon;
            int finalTeamBTricksWon  = teamBTricksWon;
            int finalTeamADeclPoints = teamADeclPoints;
            int finalTeamBDeclPoints = teamBDeclPoints;

            boolean callerIsA  = teamA.getPlayers().stream().anyMatch(p -> p.getId().equals(trumpCaller.getId()));
            int callerPts      = callerIsA ? finalTeamAHandPoints : finalTeamBHandPoints;
            int opponentPts    = callerIsA ? finalTeamBHandPoints : finalTeamAHandPoints;
            boolean padanje    = (callerPts <= opponentPts);

            applyPadanjeAndUpdateScores();

            boolean capot = (finalTeamATricksWon == 8 || finalTeamBTricksWon == 8);

            // capot bonus
            if (teamATricksWon == 8) {
                teamA.addPoints(90);
                LOGGER.info("Capot! Team A +90");
            } else if (teamBTricksWon == 8) {
                teamB.addPoints(90);
                LOGGER.info("Capot! Team B +90");
            }

            if (handCompletionCallback != null) {
                handCompletionCallback.onHandCompleted(
                        gameId,
                        finalTeamAHandPoints, finalTeamBHandPoints,
                        finalTeamADeclPoints, finalTeamBDeclPoints,
                        finalTeamATricksWon,  finalTeamBTricksWon,
                        padanje, capot
                );
            }

            // match finished?

            checkMatchEnd();
            if (gameState == GameState.COMPLETED) return;

            /* start next hand */
            resetHandState();
            resetBidding();
            startNextHand();
            return;
        }

        /* --- 3. prepare next trick – winner leads --------------------------- */
        int winIdx = turnOrder.indexOf(winner);
        Collections.rotate(turnOrder, -winIdx);    // winner becomes index 0

        currentLead   = winner;
        currentPlayer = winner;
        currentTrick  = new Trick(winnerId, trump);

        LOGGER.info("Next trick will start with {}", winnerId);
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


    private boolean isOnTeamA(Player p) {
        // Compare _IDs_, not object references
        return teamA.getPlayers()
                .stream()
                .anyMatch(t -> t.getId().equals(p.getId()));
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
     * Lets a player challenge the current hand.
     * @return true  – challenge succeeded (infraction found, points awarded, hand ended)
     *         false – either player already used their challenge OR no infraction existed
     */
    public boolean challengeHand(String playerId) {

        Player challenger = findPlayerById(playerId);
        if (challengeUsed.getOrDefault(playerId, false)) return false;
        challengeUsed.put(playerId, true);

        if (!anyFoul()) return false;                 // nobody fouled  ✗

        Team challengerTeam = getPlayerTeam(challenger);

        /* ---------- new success rule -------------------------------------- */
        boolean success =
                bothTeamsFouled()                     // either side fouled
                        || !foulingTeams.contains(challengerTeam); // only opponents fouled
        /* ------------------------------------------------------------------ */

        if (!success) {
            LOGGER.info("{} challenged but their own team was the only one that fouled", playerId);
            return false;                             // quota spent, nothing else
        }

        /* ---- award 162 + declarations to the challenger’s team ---------- */
        int declPts = (challengerTeam == teamA) ? teamADeclPoints : teamBDeclPoints;
        int total   = FULL_HAND_POINTS + declPts;

        assert challengerTeam != null;
        challengerTeam.addPoints(total);
        LOGGER.info("Challenge SUCCESS – {} pts (162+decl) awarded to {}",
                total, challengerTeam == teamA ? "Team A" : "Team B");
        checkMatchEnd();           // NEW
        if (gameState == GameState.COMPLETED) return true;

        resetHandState();
        resetBidding();
        startNextHand();
        return true;
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


    /** Move the dealer one seat clockwise and rotate turnOrder accordingly. */
    private void selectNextDealer() {
        int newDealerIndex = (turnOrder.indexOf(dealer) + 1) % turnOrder.size();
        dealer = turnOrder.get(newDealerIndex);

        // Rotate the seating list so that the player after the new dealer will bid first
        List<Player> rotated = new ArrayList<>();
        for (int i = 0; i < turnOrder.size(); i++) {
            int idx = (newDealerIndex + 1 + i) % turnOrder.size();
            rotated.add(turnOrder.get(idx));
        }
        turnOrder.clear();
        turnOrder.addAll(rotated);

        currentLead = turnOrder.getFirst();

        logTurnOrder("Dealer rotated to " + dealer.getId());

    }
    @JsonIgnore
    public void checkMatchEnd() {
        if (gameState == GameState.COMPLETED) return;     // already finished

        int a = teamA.getScore();
        int b = teamB.getScore();

        if (a < TARGET_SCORE && b < TARGET_SCORE) return; // nobody crossed yet

        if (a != b) {                     // higher score wins immediately
            gameState = GameState.COMPLETED;
            LOGGER.info("Match over – winner: {}", a > b ? "Team A" : "Team B");
        } else {
            // scores tied but both ≥ target – keep playing extra hands
            // (state stays RUNNING; UI will show “tiebreaker” automatically)
            LOGGER.info("Scores tied at >= target – starting tie-breaker hand");
        }
    }
    public boolean hasPlayerChallenged(Player p) {
        return challengeUsed.getOrDefault(p.getId(), false);
    }
    @JsonIgnore
    public List<Player> getPlayers() {
        // keep allocation cheap: the list is tiny (4)
        List<Player> all = new ArrayList<>(teamA.getPlayers());
        all.addAll(teamB.getPlayers());
        return all;
    }

    /** Starts the next hand with the next dealer in sequence (no random pick). */
    public void startNextHand() {
        selectNextDealer();      // clockwise dealer advance

        deck = new Deck();
        deck.shuffle();
        deck.dealInitialHands(turnOrder);   // 6 cards each
        talon = deck.dealTalon();           // 2 cards face-down

        gameState = GameState.BIDDING;

        logTurnOrder("New hand — dealer " + dealer.getId());
        logScores("Start of hand");
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
    /** INFO-level dump of current seating order (dealer in [ ]). */
    private void logTurnOrder(String msg) {
        String order = turnOrder.stream()
                .map(p -> (p.equals(dealer) ? "[" + p.getId() + "]" : p.getId()))
                .collect(Collectors.joining(" → "));
        LOGGER.info("{}  |  Turn order: {}", msg, order);
    }

    /** INFO-level snapshot of hand points and running scores. */
    private void logScores(String msg) {
        LOGGER.info("{}  |  HandPts A:{}  B:{}   Total A:{}  B:{}",
                msg, teamAHandPoints, teamBHandPoints,
                teamA.getScore(),    teamB.getScore());
    }
    @JsonIgnore                  // don’t let Jackson persist this
    public String getWinnerTeamId() {
        Team winner = getWinner();          // ← you already have this method
        if (winner == null) return null;    // tie / still running
        return winner == teamA ? "A" : "B";
    }

    @JsonIgnore
    public void cancelMatch() {
        if (gameState == GameState.COMPLETED) return;   // ignore finished games
        gameState = GameState.CANCELLED;
    }
    @JsonIgnore
    public void setLastActivity(Instant t) {
        this.lastActivity = t;
    }

}
