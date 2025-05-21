package backend.belatro.events;

import org.springframework.context.ApplicationEvent;

public class GameStartedEvent extends ApplicationEvent {
    private final String gameId;

    public GameStartedEvent(Object source, String gameId) {
        super(source);
        this.gameId = gameId;
    }

    public String getGameId() {
        return gameId;
    }
}