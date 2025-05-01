package backend.belatro.util;

import backend.belatro.dtos.MatchDTO;
import backend.belatro.pojo.gamelogic.*;

import java.util.List;

public final class MatchMapper {

    private MatchMapper() { }

    public static BelotGame toBelotGame(MatchDTO m) {
        List<Player> aPlayers = m.getTeamA().stream()
                .map(dto -> new Player(dto.getId()))
                .toList();
        List<Player> bPlayers = m.getTeamB().stream()
                .map(dto -> new Player(dto.getId()))
                .toList();

        Team teamA = new Team(aPlayers);
        Team teamB = new Team(bPlayers);

        BelotGame game = new BelotGame(m.getId(), teamA, teamB);
        game.startGame();                      // shuffle & deal
        return game;
    }
}
