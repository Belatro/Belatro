package backend.belatro.enums;

public enum MoveType {
    PLAY_CARD,
    BID,
    START_HAND,
    END_HAND,
    END_TRICK,
    CHALLENGE,
    SYSTEM            // fallback / internal events
}