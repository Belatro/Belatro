package backend.belatro.events;

public class GameStateChangedEvent {
    private final String gameId;
    public GameStateChangedEvent(String gameId) { this.gameId = gameId; }
    public String getGameId() { return gameId; }
}