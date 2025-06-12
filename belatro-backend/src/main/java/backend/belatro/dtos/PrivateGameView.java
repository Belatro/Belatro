package backend.belatro.dtos;

import backend.belatro.pojo.gamelogic.Card;

import java.util.List;

public record PrivateGameView(
        PublicGameView publicPart,

        List<Card> hand,          // only their own
        boolean        yourTurn,
        boolean        challengeUsed
) {}
