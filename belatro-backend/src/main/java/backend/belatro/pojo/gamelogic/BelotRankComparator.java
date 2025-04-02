package backend.belatro.pojo.gamelogic;

import backend.belatro.pojo.gamelogic.enums.Rank;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class BelotRankComparator {
        // Non-trump rank order: A, 10, K, Q, J, 9, 8, 7
        private static final Map<Rank, Integer> NON_TRUMP_RANK_ORDER = new HashMap<>();

        // Trump rank order: J, 9, A, 10, K, Q, 8, 7
        private static final Map<Rank, Integer> TRUMP_RANK_ORDER = new HashMap<>();

        private static final Map<Rank, Integer> SEQUENCE_RANK_ORDER = new HashMap<>();

        static {
            // Initialize non-trump rank order
            NON_TRUMP_RANK_ORDER.put(Rank.AS, 8);
            NON_TRUMP_RANK_ORDER.put(Rank.DESETKA, 7);
            NON_TRUMP_RANK_ORDER.put(Rank.KRALJ, 6);
            NON_TRUMP_RANK_ORDER.put(Rank.BABA, 5);
            NON_TRUMP_RANK_ORDER.put(Rank.DECKO, 4);
            NON_TRUMP_RANK_ORDER.put(Rank.DEVETKA, 3);
            NON_TRUMP_RANK_ORDER.put(Rank.OSMICA, 2);
            NON_TRUMP_RANK_ORDER.put(Rank.SEDMICA, 1);

            // Initialize trump rank order
            TRUMP_RANK_ORDER.put(Rank.DECKO, 8);
            TRUMP_RANK_ORDER.put(Rank.DEVETKA, 7);
            TRUMP_RANK_ORDER.put(Rank.AS, 6);
            TRUMP_RANK_ORDER.put(Rank.DESETKA, 5);
            TRUMP_RANK_ORDER.put(Rank.KRALJ, 4);
            TRUMP_RANK_ORDER.put(Rank.BABA, 3);
            TRUMP_RANK_ORDER.put(Rank.OSMICA, 2);
            TRUMP_RANK_ORDER.put(Rank.SEDMICA, 1);

            SEQUENCE_RANK_ORDER.put(Rank.SEDMICA, 1);
            SEQUENCE_RANK_ORDER.put(Rank.OSMICA, 2);
            SEQUENCE_RANK_ORDER.put(Rank.DEVETKA, 3);
            SEQUENCE_RANK_ORDER.put(Rank.DESETKA, 4);
            SEQUENCE_RANK_ORDER.put(Rank.DECKO, 5);
            SEQUENCE_RANK_ORDER.put(Rank.BABA, 6);
            SEQUENCE_RANK_ORDER.put(Rank.KRALJ, 7);
            SEQUENCE_RANK_ORDER.put(Rank.AS, 8);
        }


        public static Comparator<Card> getNonTrumpComparator() {
            return Comparator.comparingInt(card -> NON_TRUMP_RANK_ORDER.get(card.getRank()));
        }


        public static Comparator<Card> getTrumpComparator() {
            return Comparator.comparingInt(card -> TRUMP_RANK_ORDER.get(card.getRank()));
        }


        public static Comparator<Card> getComparator() {
            return getNonTrumpComparator();
        }

        public static final Map<Rank, Integer> RANK_ORDER = SEQUENCE_RANK_ORDER;

        public static Map<Rank, Integer> getSequenceRankOrder() {
        return Collections.unmodifiableMap(SEQUENCE_RANK_ORDER);
        }

        public static Comparator<Card> getSequenceComparator() {
        return Comparator.comparingInt(card -> SEQUENCE_RANK_ORDER.get(card.getRank()));
        }


}