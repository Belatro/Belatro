package backend.belatro.dtos;

import java.util.List;

public record MatchRatingUpdateDto(
        String                matchId,
        List<UserRatingDto> ratingChanges
) {}