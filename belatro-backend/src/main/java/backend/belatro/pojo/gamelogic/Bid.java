package backend.belatro.pojo.gamelogic;

import backend.belatro.pojo.gamelogic.enums.Boja;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a bid in the Belot card game.
 */
@NoArgsConstructor(force = true)
@Getter
public class Bid {
    public enum BidAction {
        PASS,
        CALL_TRUMP
    }
    
    private final Player player;
    private final BidAction action;
    private final Boja selectedTrump;
    
    /**
     * Creates a pass bid.
     */
    public static Bid pass(Player player) {
        return new Bid(player, BidAction.PASS, null);
    }
    
    /**
     * Creates a trump call bid.
     */
    public static Bid callTrump(Player player, Boja selectedTrump) {
        return new Bid(player, BidAction.CALL_TRUMP, selectedTrump);
    }

    @JsonCreator
    public Bid(@JsonProperty("player")       Player   player,
               @JsonProperty("action")       BidAction action,
               @JsonProperty("selectedTrump")Boja     selectedTrump) {
        if (action == BidAction.CALL_TRUMP && selectedTrump == null) {
            throw new IllegalArgumentException("Must supply trump when calling it");
        }
        this.player        = player;
        this.action        = action;
        this.selectedTrump = selectedTrump;
    }

    public boolean isPass() {
        return action == BidAction.PASS;
    }
    
    public boolean isTrumpCall() {
        return action == BidAction.CALL_TRUMP;
    }
}