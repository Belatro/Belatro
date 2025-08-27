package backend.belatro.dtos;

import backend.belatro.pojo.gamelogic.Trick;
import backend.belatro.pojo.gamelogic.enums.GameState;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.Map;

public record PublicGameView(
        String gameId,
        GameState gameState,
        @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
        List<BidDTO> bids,
        Trick currentTrick,
        int     teamAScore,
        int     teamBScore,
        List<PlayerPublicInfo> teamA,
        List<PlayerPublicInfo> teamB,
        Map<String, Boolean> challengeUsedByPlayer,
        String    winnerTeamId,     // null while running
        boolean   tieBreaker  ,
        @JsonProperty("seatingOrder")
        List<PlayerPublicInfo> seatingOrder,// true when both ≥ target AND scores equal
        Map<String, DeclarationsDTO> declarations,
        Map<String, Boolean> belaDeclaredByPlayer) {}