package backend.belatro.dtos;

import backend.belatro.pojo.gamelogic.enums.Boja;

import java.util.Map;

public record DeclarationsDTO(
        boolean bela,                    // true if K+Q of trump
        Map<Boja,Integer> sequencesBySuit, // 20/50/100 per suit (0 if none)
        Integer fourOfAKindPoints,       // 0/100/150/200
        Integer bestSequencePoints       // max of sequencesBySuit
) {}