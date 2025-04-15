package backend.belatro.pojo.gamelogic;

import backend.belatro.pojo.gamelogic.enums.Boja;
import lombok.Getter;

/**
 * Represents a bid in the Belot card game.
 */
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
    
    private Bid(Player player, BidAction action, Boja selectedTrump) {
        this.player = player;
        this.action = action;
        this.selectedTrump = selectedTrump;
        
        if (action == BidAction.CALL_TRUMP && selectedTrump == null) {
            throw new IllegalArgumentException("Must provide a trump suit when calling trump");
        }
    }

    public boolean isPass() {
        return action == BidAction.PASS;
    }
    
    public boolean isTrumpCall() {
        return action == BidAction.CALL_TRUMP;
    }
}