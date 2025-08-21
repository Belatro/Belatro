package backend.belatro.dtos;

public record HandSummaryDTO(
        int teamAPoints,
        int teamBPoints,
        int teamADeclPoints,
        int teamBDeclPoints,
        int teamATricksWon,
        int teamBTricksWon,
        boolean padanje,
        boolean capot,
        int finalScoreA,
        int finalScoreB
) {}
