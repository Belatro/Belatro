package backend.belatro.dtos;

public record PlayerMatchHistoryDTO(
        MatchHistoryDTO history,
        String           yourResult
) {}