package backend.belatro.pojo.gamelogic;

import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.Rank;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.*;

/**
 * Represents a single trick in a Belot game.
 * A trick consists of one card played by each player, with the first card played by the lead player.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Trick {
    @Getter
    private final String leadPlayerId;

    @Getter
    private final Boja trump;

    private final Map<String, Card> plays;


    @JsonCreator
    public Trick(@JsonProperty("leadPlayerId") String leadPlayerId,
                 @JsonProperty("trump")        Boja   trump) {
        this.leadPlayerId = Objects.requireNonNull(leadPlayerId);
        this.trump        = trump;
        this.plays        = new HashMap<>();
    }

    /**
     * Adds a card played by a player to this trick.
     *
     * @param playerId The ID of the player playing the card
     * @param card The card being played
     * @throws NullPointerException if playerId or card is null
     * @throws IllegalArgumentException if the player has already played a card in this trick
     */
    public void addPlay(String playerId, Card card) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(card, "Card cannot be null");

        if (plays.containsKey(playerId)) {
            throw new IllegalArgumentException("Player " + playerId + " has already played a card in this trick");
        }

        plays.put(playerId, card);
    }


    public boolean isComplete(int playerCount) {
        System.out.println("Checking if trick is complete. Current plays: " + plays.size() + ", Required: " + playerCount);
        return plays.size() == playerCount;
    }


    /**
     * @return An unmodifiable map of player IDs to cards
     */
    public Map<String, Card> getPlays() {
        return Collections.unmodifiableMap(plays);
    }

    /**
     * @return The playerID who won the trick, or null if the trick is empty
     */
    public String determineWinner() {
        if (plays.isEmpty()) {
            return null;
        }

        Card leadCard = plays.get(leadPlayerId);
        if (leadCard == null) {
            throw new IllegalStateException("Lead player has not played a card");
        }

        Boja leadSuit = leadCard.getBoja();

        String currentWinnerId = leadPlayerId;
        Card currentWinningCard = leadCard;

        for (Map.Entry<String, Card> entry : plays.entrySet()) {
            String playerId = entry.getKey();
            Card card = entry.getValue();

            // Skip the lead player, already considered
            if (playerId.equals(leadPlayerId)) {
                continue;
            }

            // Compare cards to determine the winner
            if (!isCardWinning(currentWinningCard, card, leadSuit)) {
                currentWinnerId = playerId;
                currentWinningCard = card;
            }
        }

        return currentWinnerId;
    }

    /**
     * Determines if the winning card beats the challenger card.
     *
     * @param winningCard The current winning card
     * @param challengerCard The challenger card
     * @param leadSuit The suit of the lead card
     * @return true if the winning card beats the challenger, false otherwise
     */
    private boolean isCardWinning(Card winningCard, Card challengerCard, Boja leadSuit) {
        // If winning card is trump and challenger is not, winning card wins
        if (winningCard.getBoja() == trump && challengerCard.getBoja() != trump) {
            return true;
        }

        // If challenger is trump and winning card is not, challenger wins
        if (challengerCard.getBoja() == trump && winningCard.getBoja() != trump) {
            return false;
        }

        // If both are trump, higher rank wins
        if (winningCard.getBoja() == trump) {
            Comparator<Card> trumpComparator = BelotRankComparator.getTrumpComparator();
            return trumpComparator.compare(winningCard, challengerCard) > 0;
        }

        // If winning card follows lead suit and challenger doesn't, winning card wins
        if (winningCard.getBoja() == leadSuit && challengerCard.getBoja() != leadSuit) {
            return true;
        }

        // If challenger follows lead suit and winning card doesn't, challenger wins
        if (challengerCard.getBoja() == leadSuit && winningCard.getBoja() != leadSuit) {
            return false;
        }

        // If both follow lead suit, higher rank wins
        if (winningCard.getBoja() == leadSuit) {
            Comparator<Card> nonTrumpComparator = BelotRankComparator.getNonTrumpComparator();
            return nonTrumpComparator.compare(winningCard, challengerCard) > 0;
        }

        // If neither follows lead suit or trump, winning card stays winning
        return true;
    }

    /**
     * Calculates the total point value of all cards in this trick.
     *
     * @return The total point value
     */
    public int calculatePoints() {
        return plays.values().stream()
            .mapToInt(this::getCardPointValue)
            .sum();
    }

    /**
     * Gets the point value of a card based on its rank and whether it's trump.
     *
     * @param card The card to evaluate
     * @return The point value of the card
     */
    private int getCardPointValue(Card card) {
        boolean isTrump = card.getBoja() == trump;
        Rank rank = card.getRank();

        // Belot card point values
        if (isTrump) {
            return switch (rank) {
                case DECKO -> 20;  // Jack
                case DEVETKA -> 14;  // Nine
                case AS -> 11;  // Ace
                case DESETKA -> 10;  // Ten
                case KRALJ -> 4;  // King
                case BABA -> 3;  // Queen
                default -> 0;
            };
        } else {
            return switch (rank) {
                case AS -> 11;  // Ace
                case DESETKA -> 10;  // Ten
                case KRALJ -> 4;  // King
                case BABA -> 3;  // Queen
                case DECKO -> 2;  // Jack
                default -> 0;
            };
        }
    }

    /**
     * @return the Card that is currently winning this trick,
     *         or null if no cards have been played yet.
     */
    public Card getWinningCard() {
        if (plays.isEmpty()) {
            return null;
        }

        Card winningCard = getLeadCard();
        Boja leadSuit = winningCard.getBoja();

        for (Card challenger : plays.values()) {
            // skip comparing the lead against itself
            if (challenger == winningCard) {
                continue;
            }
            // if challenger beats what we thought was winning, adopt it
            if (!isCardWinning(winningCard, challenger, leadSuit)) {
                winningCard = challenger;
            }
        }

        return winningCard;
    }

    /**
     * Returns the last card played in this trick.
     *
     * @return The last card played, or null if no cards have been played
     */
    public Card getLastCardPlayed() {
        if (plays.isEmpty()) {
            return null;
        }

        // This assumes the map iteration order reflects insertion order,
        // which is true for LinkedHashMap but not guaranteed for HashMap
        Card lastCard = null;
        for (Card card : plays.values()) {
            lastCard = card;
        }
        return lastCard;
    }

    /**
     * Returns the lead card of this trick.
     *
     * @return The lead card, or null if the lead player hasn't played yet
     */
    public Card getLeadCard() {
        return plays.get(leadPlayerId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Trick [leadPlayer=").append(leadPlayerId)
            .append(", trump=").append(trump)
            .append(", plays={");

        boolean first = true;
        for (Map.Entry<String, Card> entry : plays.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }

        return sb.append("}]").toString();
    }
}