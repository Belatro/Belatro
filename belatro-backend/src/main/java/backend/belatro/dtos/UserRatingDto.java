package backend.belatro.dtos;

import lombok.Data;

@Data
public class UserRatingDto {
    private String userId;
    private int oldRating;
    private int newRating;
    private int gamesPlayed;
}
