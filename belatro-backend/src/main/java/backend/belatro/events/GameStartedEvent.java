package backend.belatro.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class GameStartedEvent extends ApplicationEvent {
    private final String gameId;

    public GameStartedEvent(Object source, String gameId) {
        super(source);
        this.gameId = gameId;
    }

}