package backend.belatro.dtos;

import backend.belatro.enums.GameMode;

import java.time.Instant;

public record PlayerMatchSummaryDTO(
        String  matchId,
        Instant endTime,
        String  result,
        String  yourOutcome,
        GameMode gameMode
) {}
