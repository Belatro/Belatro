package backend.belatro.dtos;

import java.util.List;

public record TrickDTO(int trickNo,
                       String trump,        // e.g. "♠" (null if no trump)
                       List<MoveDTO> moves) {}