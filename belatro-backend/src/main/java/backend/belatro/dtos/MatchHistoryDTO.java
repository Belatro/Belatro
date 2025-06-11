package backend.belatro.dtos;

import java.util.List;

public record MatchHistoryDTO(
        MatchDTO match,
        List<MoveDTO> moves,
        List<HandDTO> structuredMoves
) {}