package backend.belatro.dtos;

import backend.belatro.pojo.gamelogic.enums.Boja;

public record BidDTO(
        String    playerId,
        String    action,
        Boja selectedTrump
) {}