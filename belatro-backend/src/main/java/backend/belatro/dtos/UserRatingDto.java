package backend.belatro.dtos;

public record UserRatingDto(
        String userId,
        int oldRating,
        int newRating,
        int gamesPlayed
        ) {


}
