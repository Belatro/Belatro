package backend.belatro.dtos;

import backend.belatro.pojo.gamelogic.Trick;
import backend.belatro.pojo.gamelogic.enums.GameState;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

public record PublicGameView(
        String gameId,
        GameState gameState,
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        List<BidDTO> bids,
        Trick currentTrick,
        int     teamAScore,
        int     teamBScore
) {}