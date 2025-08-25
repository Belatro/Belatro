package backend.belatro.dtos;

public record MoveDTO(int order,
                      String player,
                      Boolean legal,
                      String card) {}  // e.g. "Q♠"