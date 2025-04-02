package backend.belatro.pojo.gamelogic;

import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.GameState;
import lombok.Getter;

import java.util.*;

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

    private Deck deck;

    @Getter
    private List<Card> talon = new ArrayList<>();

    @Getter
    private final List<Trick> completedTricks = new ArrayList<>();

    @Getter
    private Trick currentTrick;

    @Getter
    private Player currentLead;

    @Getter
    private Player dealer;

    @Getter
    private Boja trump; // The selected trump suit

    private boolean trumpCalled = false;

    @Getter
    private GameState gameState = GameState.INITIALIZED;


    public BelotGame(String gameId, Team teamA, Team teamB) {
        this.gameId = Objects.requireNonNull(gameId, "Game ID cannot be null");
        this.teamA = Objects.requireNonNull(teamA, "Team A cannot be null");
        this.teamB = Objects.requireNonNull(teamB, "Team B cannot be null");

        // Build a turn order: all players in clockwise order
        turnOrder.addAll(teamA.getPlayers());
        turnOrder.addAll(teamB.getPlayers());
    }

    public void selectDealer() {
        Random random = new Random();
        int dealerIndex = random.nextInt(turnOrder.size());
        dealer = turnOrder.get(dealerIndex);

        // Reorder the turn list to start with the player after the dealer
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
        if (gameState != GameState.INITIALIZED) {
            throw new IllegalStateException("Game has already started");
        }

        gameState = GameState.DEALING;

        selectDealer();

        deck = new Deck();
        deck.shuffle();

        int cardsPerPlayer = 6;
        for (Player player : turnOrder) {
            player.setHand(deck.deal(cardsPerPlayer));
        }

        talon = deck.deal(2);

        gameState = GameState.TRUMP_CALLING;
    }

    /**
     * Allows a player to call trump.
     *
     * @param player        The player calling trump
     * @param selectedTrump The selected trump suit
     */
    public void callTrump(Player player, Boja selectedTrump) {
        if (gameState != GameState.TRUMP_CALLING) {
            throw new IllegalStateException("Cannot call trump when game is not in trump calling phase");
        }

        if (trumpCalled) {
            return;
        }

        this.trump = selectedTrump;
        trumpCalled = true;

        dealRemainingCards();

        processDeclarations();

        currentTrick = new Trick(currentLead.getId(), trump);

        gameState = GameState.PLAYING_TRICKS;
    }

    /**
     * Deals the remaining cards to players after trump is called.
     */
    private void dealRemainingCards() {
        int remainingCardsPerPlayer = 2;
        for (Player player : turnOrder) {
            List<Card> additionalCards = deck.deal(remainingCardsPerPlayer);
            player.getHand().addAll(additionalCards);
        }
    }

    /**
     * Process all declarations from players and determine which ones count.
     */
    private void processDeclarations() {
        Map<Player, Integer> declarations = new HashMap<>();

        for (Player player : turnOrder) {
            ZvanjaValidator.evaluateSequence(player.getHand(), trump)
                .ifPresent(points -> declarations.put(player, points));
        }

        List<Player> teamAPlayers = teamA.getPlayers();
        List<Player> teamBPlayers = teamB.getPlayers();

        int teamAPoints = 0;
        int teamBPoints = 0;

        for (Map.Entry<Player, Integer> entry : declarations.entrySet()) {
            Player player = entry.getKey();
            int points = entry.getValue();

            if (teamAPlayers.contains(player)) {
                teamAPoints += points;
            } else if (teamBPlayers.contains(player)) {
                teamBPoints += points;
            }
        }

        if (teamAPoints > teamBPoints) {
            for (Map.Entry<Player, Integer> entry : declarations.entrySet()) {
                Player player = entry.getKey();
                int points = entry.getValue();

                if (teamAPlayers.contains(player)) {
                    teamA.addPoints(points);
                }
            }
        } else if (teamBPoints > teamAPoints) {
            for (Map.Entry<Player, Integer> entry : declarations.entrySet()) {
                Player player = entry.getKey();
                int points = entry.getValue();

                if (teamBPlayers.contains(player)) {
                    teamB.addPoints(points);
                }
            }
        } else if (teamAPoints > 0) {
            for (Map.Entry<Player, Integer> entry : declarations.entrySet()) {
                Player player = entry.getKey();
                int points = entry.getValue();

                if (teamAPlayers.contains(player)) {
                    teamA.addPoints(points);
                } else if (teamBPlayers.contains(player)) {
                    teamB.addPoints(points);
                }
            }
        }

        checkBelaDeclarations();
    }


    private void checkBelaDeclarations() {
        for (Player player : turnOrder) {
            if (ZvanjaValidator.isBela(player.getHand(), trump)) {
                // Award 20 points for Bela
                if (teamA.getPlayers().contains(player)) {
                    teamA.addPoints(20);
                } else if (teamB.getPlayers().contains(player)) {
                    teamB.addPoints(20);
                }
            }
        }
    }

    /**

     * @param player The player playing the card
     * @param card The card being played
     * @return true if the card was played successfully, false otherwise
     */
    public boolean playCard(Player player, Card card) {
        if (gameState != GameState.PLAYING_TRICKS) {
            throw new IllegalStateException("Cannot play card when game is not in playing tricks phase");
        }

        if (!isPlayerTurn(player)) {
            return false;
        }

        if (!player.getHand().contains(card)) {
            return false;
        }

        if (!isValidPlay(player, card)) {
            return false;
        }

        player.playCard(card);
        currentTrick.addPlay(player.getId(), card);

        if (currentTrick.isComplete(turnOrder.size())) {
            String trickWinnerId = currentTrick.determineWinner();
            Player trickWinner = turnOrder.stream()
                    .filter(p -> p.getId().equals(trickWinnerId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Winner not found in turn order"));

            int trickPoints = currentTrick.calculatePoints();
            updateScores(trickWinner, trickPoints);

            completedTricks.add(currentTrick);

            currentLead = trickWinner;

            if (completedTricks.size() == 8) {
                finalizeRound();
            } else {

                currentTrick = new Trick(currentLead.getId(), trump);
            }
        }

        return true;
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


    private void finalizeRound() {

        String lastTrickWinnerId = completedTricks.getLast().determineWinner();
        Player lastTrickWinner = turnOrder.stream()
                .filter(p -> p.getId().equals(lastTrickWinnerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Last trick winner not found"));

        if (teamA.getPlayers().contains(lastTrickWinner)) {
            teamA.addPoints(10); // Last trick bonus
        } else {
            teamB.addPoints(10); // Last trick bonus
        }

        // Game is over
        gameState = GameState.ROUND_ENDED;
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
        if (gameState != GameState.ROUND_ENDED) {
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
     * @return A string representation of the game state
     */
    public String getGameStateDescription() {
        return switch (gameState) {
            case INITIALIZED -> "Game initialized";
            case DEALING -> "Dealing cards";
            case TRUMP_CALLING -> "Waiting for trump to be called";
            case PLAYING_TRICKS -> "Playing tricks";
            case ROUND_ENDED -> "Round ended";
        };
    }

    /**
     * @return The current player, or null if the game hasn't started or is over
     */
    public Player getCurrentPlayer() {
        if (gameState != GameState.PLAYING_TRICKS) {
            return null;
        }

        if (currentTrick.getPlays().isEmpty()) {
            return currentLead;
        }

        for (Player p : turnOrder) {
            if (!currentTrick.getPlays().containsKey(p.getId())) {
                return p;
            }
        }

        return null;
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
    public Map<String, Card> getCurrentTrickPlays() {
        return currentTrick != null ? currentTrick.getPlays() : Collections.emptyMap();
    }

    /**
     * @return A list of cards that the current player can legally play
     */
    public List<Card> getLegalMoves() {
        Player currentPlayer = getCurrentPlayer();
        if (currentPlayer == null) {
            return Collections.emptyList();
        }

        List<Card> hand = currentPlayer.getHand();

        if (currentTrick.getPlays().isEmpty()) {
            return new ArrayList<>(hand);
        }

        Card leadCard = currentTrick.getLeadCard();
        Boja leadSuit = leadCard.getBoja();

        List<Card> leadSuitCards = hand.stream()
                .filter(c -> c.getBoja() == leadSuit)
                .toList();

        if (!leadSuitCards.isEmpty()) {
            return leadSuitCards;
        }

        boolean trumpInTrick = currentTrick.getPlays().values().stream()
                .anyMatch(c -> c.getBoja() == trump);

        if (trumpInTrick) {
            List<Card> trumpCards = hand.stream()
                    .filter(c -> c.getBoja() == trump)
                    .toList();

            if (!trumpCards.isEmpty()) {
                return trumpCards;
            }
        }

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
