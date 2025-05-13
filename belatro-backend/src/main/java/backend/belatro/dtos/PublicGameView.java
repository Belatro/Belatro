package backend.belatro.dtos;

import backend.belatro.pojo.gamelogic.Bid;
import backend.belatro.pojo.gamelogic.Trick;
import backend.belatro.pojo.gamelogic.enums.GameState;

import java.util.List;

public record PublicGameView(
        String gameId,
        GameState gameState,
        List<Bid> bids,
        Trick currentTrick,
        int     teamAScore,
        int     teamBScore
) {}