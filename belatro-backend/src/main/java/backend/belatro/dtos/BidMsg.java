package backend.belatro.dtos;

import backend.belatro.pojo.gamelogic.enums.Boja;

public record BidMsg(
        String playerId,
        boolean pass,      // true = “Pass”, false = “Call”
        Boja trump         // ignored when pass==true
) { }