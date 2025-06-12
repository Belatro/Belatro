package backend.belatro.dtos;

public record MoveDTO(int order,
                      String player,   // username
                      String card) {}  // e.g. "Q♠"