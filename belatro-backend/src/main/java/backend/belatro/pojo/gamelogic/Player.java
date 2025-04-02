package backend.belatro.pojo.gamelogic;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Player {
    private final String id;
    private final List<Card> hand = new ArrayList<>();

    public Player(String id) {
        this.id = id;
    }

    public void setHand(List<Card> cards) {
        hand.clear();
        hand.addAll(cards);
    }

    public Card playCard(Card card) {
        if (!hand.contains(card)) {
            throw new IllegalArgumentException("Card not in hand");
        }
        hand.remove(card);
        return card;
    }
}
