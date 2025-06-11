package backend.belatro.dtos;

import java.util.Set;

public record RematchVoteEvent(String oldGameId, Set<String> accepted) {
}
