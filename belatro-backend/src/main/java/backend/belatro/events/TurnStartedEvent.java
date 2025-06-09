package backend.belatro.events;

import backend.belatro.pojo.gamelogic.enums.GameState;

/**
 * @param phase BIDDING vs PLAYING
 */

public record TurnStartedEvent(String matchId, String playerId, GameState phase) {

}