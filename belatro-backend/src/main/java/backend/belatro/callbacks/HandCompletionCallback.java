package backend.belatro.callbacks;

public interface HandCompletionCallback {
    void onHandCompleted(String gameId,
                         int teamAHandPoints, int teamBHandPoints,
                         int teamADeclPoints, int teamBDeclPoints,
                         int teamATricksWon, int teamBTricksWon,
                         boolean padanje, boolean capot);
}
