package backend.belatro.pojo.gamelogic;


import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.Rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private final List<Card> cards = new ArrayList<>();
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



}
