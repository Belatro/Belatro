package backend.belatro.pojo.gamelogic;

import lombok.Getter;

import java.util.List;

@Getter
public class Team {
    private final List<Player> players;
    private int score;

    public Team(List<Player> players) {
        if (players.size() != 2) {
            throw new IllegalArgumentException("Each team must have exactly 2 players");
        }
        this.players = players;
        this.score = 0;
    }

    public void addPoints(int points) {
        this.score += points;
    }
}
