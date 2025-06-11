package backend.belatro.dtos;

import backend.belatro.enums.QueueState;

/**
 * Immutable status frame the backend sends to
 * /user/queue/ranked/status while a player waits in the ranked queue.
 *
 *  state           – IN_QUEUE | MATCH_FOUND | CANCELLED | ERROR
 *  estWaitSeconds  – -1 if unknown
 *  queueSize       – cosmetic (“123 players waiting”)
 *  mmr             – player’s current rating
 *  rank            – Bronze / Silver / …
 *  rankProgress    – 0–1 toward next tier
 *  matchId         – present only when state == MATCH_FOUND
 */
public record QueueStatusDTO(
        QueueState state,
        int        estWaitSeconds,
        int        queueSize,
        int        mmr,
        String     matchId
) {}
