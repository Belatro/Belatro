package backend.belatro.dtos;

import java.time.Instant;

public record PlayerMatchSummaryDTO(
        String  matchId,
        Instant endTime,
        String  result,
        String  yourOutcome   // "WIN" or "LOSS"
) {}
