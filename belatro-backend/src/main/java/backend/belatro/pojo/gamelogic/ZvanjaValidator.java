package backend.belatro.pojo.gamelogic;

import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.Rank;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    /**
     * Evaluates all sequences across all suits in a player's hand.
     * @param hand The player's hand
     * @return Map of suit to sequence points for all valid sequences
     */
    public static Map<Boja, Integer> evaluateAllSequences(List<Card> hand) {
        Map<Boja, Integer> sequencesByColor = new HashMap<>();

        // Check sequences in each suit
        for (Boja suit : Boja.values()) {
            List<Card> suitCards = hand.stream()
                    .filter(card -> card.getBoja() == suit)
                    .sorted(BelotRankComparator.getSequenceComparator())
                    .toList();

            detectSequences(suitCards).ifPresent(points ->
                    sequencesByColor.put(suit, points));
        }

        return sequencesByColor;
    }
    /**
     * Evaluates four-of-a-kind combinations in a player's hand.
     * @param hand The player's hand
     * @return Optional containing points if a valid four-of-a-kind is found
     */
    public static Optional<Integer> evaluateFourOfAKind(List<Card> hand) {
        Map<Rank, Long> rankCounts = hand.stream()
                .collect(Collectors.groupingBy(Card::getRank, Collectors.counting()));

        for (Map.Entry<Rank, Long> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 4) {
                Rank rank = entry.getKey();
                if (rank == Rank.DECKO) return Optional.of(200);
                if (rank == Rank.DEVETKA) return Optional.of(150);
                if (rank == Rank.AS || rank == Rank.DESETKA ||
                        rank == Rank.KRALJ || rank == Rank.BABA)
                    return Optional.of(100);
            }
        }
        return Optional.empty();
    }


}
