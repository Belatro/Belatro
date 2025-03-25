package backend.belatro.components;

import backend.belatro.models.User;
import org.springframework.stereotype.Component;

@Component
public class EloRatingCalculator {

    // napravljena padajuca exponencijalna funkcija pomocu lambde:
    // za n = 0: B(0)=50, M(0)=0.25
    // za n = 20: B(n) ~ 10, M(n) ~ 0.05.
    private static final double LAMBDA = 0.1844;


    public double baseline(int gamesPlayed) {
        return 40 * Math.exp(-LAMBDA * gamesPlayed) + 10;
    }


    public double multiplier(int gamesPlayed) {
        return 0.20 * Math.exp(-LAMBDA * gamesPlayed) + 0.05;
    }

    /**
     * alogritam je sljedeci
     * razlikau u ratingu:
     *     d = (avg rating protivnika) - (your rating).
     * ako dobijes:
     *     ΔR = B(n) + M(n) * d
     * ili ako izgubis:
     *     ΔR = -B(n) + M(n) * d
     *
     * final rating je currentRating + ΔR (roundano).
     */
    public int computeNewRating(User user, double opponentAvg, boolean win) {
        int gamesPlayed = user.getGamesPlayed();
        double currentRating = user.getEloRating();
        double B = baseline(gamesPlayed);
        double M = multiplier(gamesPlayed);
        double d = opponentAvg - currentRating;
        double delta = (win ? B : -B) + M * d;
        return (int) Math.round(currentRating + delta);
    }
}
