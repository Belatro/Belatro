package backend.belatro.dtos;

import java.util.List;

/**
 * One trick inside a hand – just its order number and the plays.
 */

public record TrickDTO(int trickNo,
                       List<MoveDTO> moves) {}
