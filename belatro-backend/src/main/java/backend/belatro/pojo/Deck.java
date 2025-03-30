package backend.belatro.pojo;


import backend.belatro.pojo.enums.Boja;
import backend.belatro.pojo.enums.Rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private final List<Karta> karte = new ArrayList<>();
    public Deck() {
        for (Boja boja : Boja.values()) {
            for (Rank rank : Rank.values()) {
                karte.add(new Karta(boja, rank));
            }
        }
    }
    public void shuffle() {
        Collections.shuffle(karte);
    }
    public List<Karta> deal(int count) {
        if (karte.size() < count) {
            throw new IllegalStateException("Not enough cards to deal");
        }
        List<Karta> hand = new ArrayList<>(karte.subList(0, count));
        karte.subList(0, count).clear();
        return hand;
    }



}
