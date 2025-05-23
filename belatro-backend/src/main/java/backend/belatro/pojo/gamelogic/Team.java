package backend.belatro.pojo.gamelogic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Team {
    private final List<Player> players;
    @JsonProperty("Score")
    private int score;

    @JsonCreator
    public Team(@JsonProperty("players") List<Player> players) {
        if (players.size() != 2) {
            throw new IllegalArgumentException("Each team must have exactly 2 players");
        }
        this.players = players;
        this.score   = 0;
    }

    public void addPoints(int points) {
        this.score += points;
    }
}
