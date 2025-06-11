package backend.belatro;

import backend.belatro.components.EloRatingCalculator;
import backend.belatro.models.User;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
class EloRatingCalculatorTest {

    private final EloRatingCalculator calc = new EloRatingCalculator();

    @ParameterizedTest
    @CsvSource({
            // n, rating, oppAvg, win, expectedDelta
            "0, 1000, 1000, true,  50",
            "0, 1000, 1000, false, -50",
            "20,1000, 1100, true,  17",   // 100-pt under-dog win
            "20,1000,  900, false, -17",  // favourite loss
    })
    void computesExpectedDelta(int n, int r, int opp, boolean win, int expectedDelta) {
        User u = new User();
        u.setEloRating(r);
        u.setGamesPlayed(n);

        int newRating = calc.computeNewRating(u, opp, win);

        assertThat(newRating - r).isEqualTo(expectedDelta);
    }
}
