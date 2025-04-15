package backend.belatro.pojo.gamelogic.enums;

/**
 * Represents the different states of a Belot game.
 */
public enum GameState {
    /**
     * The game has been created but not yet started.
     */
    INITIALIZED,

    /**
     * Cards have been dealt, and players are bidding for trump.
     */
    BIDDING,

    /**
     * Trump has been determined, and players are declaring combinations.
     */
    DECLARATIONS,

    /**
     * The declaration phase has ended, and players are playing tricks.
     */
    PLAYING,

    /**
     * All tricks have been played, and the round is being scored.
     */
    SCORING,

    /**
     * The game has been completed.
     */
    COMPLETED;

    /**
     * @return A user-friendly description of the game state
     */
    public String getDescription() {
        return switch (this) {
            case INITIALIZED -> "Game is initialized but not yet started";
            case BIDDING -> "Players are bidding for trump";
            case DECLARATIONS -> "Players are declaring combinations";
            case PLAYING -> "Players are playing tricks";
            case SCORING -> "Round is being scored";
            case COMPLETED -> "Game is completed";
        };
    }
}