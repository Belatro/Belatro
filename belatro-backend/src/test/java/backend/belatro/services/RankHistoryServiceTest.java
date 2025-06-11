package backend.belatro.services;

import backend.belatro.components.EloRatingCalculator;
import backend.belatro.dtos.MatchRatingUpdateDto;
import backend.belatro.enums.GameMode;
import backend.belatro.models.Match;
import backend.belatro.models.RankHistory;
import backend.belatro.models.User;
import backend.belatro.repos.RankHistoryRepo;
import backend.belatro.repos.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)           // disables Redis etc.
class RankHistoryServiceTest {

    @Mock
    UserRepo        userRepo;
    @Mock  RankHistoryRepo rankHistoryRepo;

    @Spy
    EloRatingCalculator eloCalc = new EloRatingCalculator();

    @InjectMocks            // <-- this wires mocks + spy into the service
    RankHistoryService service;



    @Test
    void updatesBothTeamsAndWritesHistory() {
        // Arrange two users per team
        User a1 = user("A1", 1000, 20);
        User a2 = user("A2", 1000, 20);
        User b1 = user("B1", 1000, 20);
        User b2 = user("B2", 1000, 20);

        Match match = match(List.of(a1, a2), List.of(b1, b2), "A", GameMode.RANKED);

        // Act
        MatchRatingUpdateDto dto = service.updateRatingsForMatch(match);

        // Assert 4 history entries saved
        verify(rankHistoryRepo, times(4)).save(any(RankHistory.class));

        // Ratings changed symmetrical ±10
        dto.ratingChanges().forEach(rc ->
                assertThat(Math.abs(rc.newRating() - rc.oldRating())).isEqualTo(10));
    }

    /* ===== helpers for RankHistoryServiceTest ===== */

    private static User user(String id, int elo, int games) {
        User u = new User();
        u.setId(id);                // any unique string
        u.setUsername(id);          // fine for a test
        u.setEloRating(elo);
        u.setGamesPlayed(games);
        return u;
    }

    /**
     * Builds a Match with just the pieces RankHistoryService needs:
     *  • teamA / teamB lists
     *  • gameMode
     *  • result  ("A" or "B")
     *  • id      (optional but nice for DTO assertions)
     */
    private static Match match(List<User> teamA,
                               List<User> teamB,
                               String     winnerTeam,      // "A" or "B"
                               GameMode   mode) {

        Match m = new Match();
        m.setId(UUID.randomUUID().toString());
        m.setTeamA(teamA);
        m.setTeamB(teamB);
        m.setGameMode(mode);
        m.setResult(winnerTeam);     // RankHistoryService looks at this
        return m;
    }

}
