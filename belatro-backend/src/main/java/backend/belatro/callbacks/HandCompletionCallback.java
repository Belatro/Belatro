package backend.belatro.callbacks;

import backend.belatro.pojo.gamelogic.BelotGame;

@FunctionalInterface
public interface HandCompletionCallback {
    void onHandCompleted(
            BelotGame game,
            int teamAHandPoints,
            int teamBHandPoints,
            int teamADeclPoints,
            int teamBDeclPoints,
            int teamATricksWon,
            int teamBTricksWon,
            boolean padanje,
            boolean capot
    );
}
