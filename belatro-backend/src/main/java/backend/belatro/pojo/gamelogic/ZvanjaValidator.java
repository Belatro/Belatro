package backend.belatro.pojo.gamelogic;

import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.Rank;

import java.util.List;
import java.util.Optional;

public class ZvanjaValidator {
    public static boolean isBela(List<Card> hand, Boja adut) {
        boolean hasKralj = hand
                .stream()
                .anyMatch(card -> card
                        .getBoja() == adut
                        &&
                        card.getRank() == Rank.KRALJ);
        boolean hasBaba = hand
                .stream()
                .anyMatch(card -> card
                        .getBoja() == adut
                        &&
                        card.getRank() == Rank.BABA);
        return hasKralj && hasBaba;
    }
    // tris, cetri, pet
    public static Optional<Integer> evaluateSequence(List<Card> hand, Boja boja) {
        List<Card> suitCards = hand.stream()
                .filter(card -> card.getBoja() == boja)
                .sorted(BelotRankComparator.getComparator()) // You may want to use a custom order.
                .toList();

        return detectSequences(suitCards);
    }
    private static Optional<Integer> detectSequences(List<Card> suitCards) {
        if (suitCards.size() < 3) {
            return Optional.empty();
        }


        int longestRun = 1;
        int maxPoints = 0;
        int currentRun = 1;

        for (int i = 1; i < suitCards.size(); i++) {
            int previousRankValue = BelotRankComparator.getSequenceRankOrder().get(suitCards.get(i - 1).getRank());
            int currentRankValue = BelotRankComparator.getSequenceRankOrder().get(suitCards.get(i).getRank());


            if (currentRankValue == previousRankValue + 1) {

                currentRun++;
            } else if (currentRankValue != previousRankValue) {
                currentRun = 1;
            }


            longestRun = Math.max(longestRun, currentRun);
        }


        if (longestRun >= 5) {
            maxPoints = 100;
        } else if (longestRun == 4) {
            maxPoints = 50;
        } else if (longestRun == 3) {
            maxPoints = 20;
        }

        return maxPoints > 0 ? Optional.of(maxPoints) : Optional.empty();
    }
}
