package backend.belatro.dtos;

import backend.belatro.pojo.gamelogic.Card;

public record PlayCardMsg(
        String playerId,
        Card   card,
        boolean declareBela
) {}
