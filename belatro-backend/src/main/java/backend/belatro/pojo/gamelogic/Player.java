package backend.belatro.pojo.gamelogic;

import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id")
public class Player {
    private final String id;
    private final List<Card> hand = new ArrayList<>();
    @Setter
    private boolean bidPassed = false;
    @JsonIgnore
    private List<Bid> bidsHistory = new ArrayList<>();

    @JsonCreator
    public Player(@JsonProperty("id") String id) {
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

    /**
     * Checks if this player has passed during the current bidding round.
     */
    public boolean hasBidPassed() {
        return bidPassed;
    }

    /**
     * Records a bid made by this player.
     */
    public void recordBid(Bid bid) {
        if (bid.getPlayer() != this) {
            throw new IllegalArgumentException("Cannot record a bid for a different player");
        }

        bidsHistory.add(bid);

        if (bid.isPass()) {
            bidPassed = true;
        }
    }
    @JsonIgnore
    public Bid getLastBid() {
        if (bidsHistory.isEmpty()) {
            return null;
        }
        return bidsHistory.get(bidsHistory.size() - 1);
    }
    public void resetBidding() {
        bidPassed = false;
        bidsHistory.clear();
    }



}
