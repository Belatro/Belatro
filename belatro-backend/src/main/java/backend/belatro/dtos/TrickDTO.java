package backend.belatro.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * One trick inside a hand – order number, plays, winner, points and optional last trick bonus.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TrickDTO(
        int trickNo,
        String winnerId,
        Integer points,
        List<MoveDTO> moves,
        Boolean lastTrickBonus
) {}
