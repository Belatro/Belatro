package backend.belatro.pojo.gamelogic;

import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.Rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    final List<Card> cards = new ArrayList<>();

    public Deck() {
        for (Boja boja : Boja.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(boja, rank));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public List<Card> deal(int count) {
        if (cards.size() < count) {
            throw new IllegalStateException("Not enough cards to deal");
        }
        List<Card> hand = new ArrayList<>(cards.subList(0, count));
        cards.subList(0, count).clear();
        return hand;
    }

    /**
     * Deals initial hands of 6 cards to each of the 4 players
     * @param players The list of players in the game
     */
    public void dealInitialHands(List<Player> players) {
        if (players.size() != 4) {
            throw new IllegalArgumentException("Belot requires exactly 4 players");
        }

        if (cards.size() < 26) { // 24 for players + 2 for talon
            throw new IllegalStateException("Not enough cards for initial deal");
        }

        // Deal 6 cards to each player
        for (Player player : players) {
            List<Card> hand = deal(6);
            player.setHand(hand);
        }
    }

    /**
     * Sets aside 2 cards for the talon
     * @return The 2 cards for the talon
     */
    public List<Card> dealTalon() {
        // Deal 2 cards per player (8 total) for the talon
        if (cards.size() < 8) {
            throw new IllegalStateException("Not enough cards for talon");
        }

        return deal(8);
    }


    /**
     * Deals the remaining cards after trump is called
     * @param players The list of players in the game
     */
    public void dealRemainingCards(List<Player> players) {
        if (players.size() != 4) {
            throw new IllegalArgumentException("Belot requires exactly 4 players");
        }

        if (cards.size() != 8) {
            throw new IllegalStateException("Expected exactly 8 cards remaining for second deal");
        }

        // Each player gets 2 more cards
        for (Player player : players) {
            List<Card> currentHand = new ArrayList<>(player.getHand());
            currentHand.addAll(deal(2));
            player.setHand(currentHand);
        }
    }

    /**
     * @return The number of cards remaining in the deck
     */
    public int getCardsRemaining() {
        return cards.size();
    }

    /**
     * @return Whether the deck is empty
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }
}