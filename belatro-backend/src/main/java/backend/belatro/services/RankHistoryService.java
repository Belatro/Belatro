package backend.belatro.services;

import backend.belatro.components.EloRatingCalculator;
import backend.belatro.dtos.MatchRatingUpdateDto;
import backend.belatro.dtos.UserRatingDto;
import backend.belatro.enums.GameMode;
import backend.belatro.models.Match;
import backend.belatro.models.RankHistory;
import backend.belatro.models.User;
import backend.belatro.repos.RankHistoryRepo;
import backend.belatro.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RankHistoryService {
    private final RankHistoryRepo rankHistoryRepo;
    private final UserRepo userRepo;
    private final EloRatingCalculator eloCalc;

    /**
     * Processes a finished match and updates player Elo ratings if ranked.
     * @param match the Match that just ended (with result and teams populated).
     * @return a DTO summarizing Elo changes (if needed), or null for casual matches.
     */
    public MatchRatingUpdateDto updateRatingsForMatch(Match match) {
        // Only update ratings for ranked matches
        if (match.getGameMode() != GameMode.RANKED) {
            return null; // No rating changes for casual games
        }
        String winnerTeam = match.getResult();  // e.g. "A" or "B"
        if (winnerTeam == null) {
            throw new IllegalStateException("Match result is null – cannot update ratings");
        }

        // Load the full user objects for each player in Team A and Team B
        List<User> teamAPlayers = match.getTeamA();
        List<User> teamBPlayers = match.getTeamB();
        // (If match.getTeamA() returns only proxies or IDs, you may need to fetch from userRepo by ID)

        // Calculate average ratings for each team before the update
        double teamAAvg = teamAPlayers.stream().mapToInt(User::getEloRating).average().orElse(0);
        double teamBAvg = teamBPlayers.stream().mapToInt(User::getEloRating).average().orElse(0);

        // Prepare a list to accumulate the rating changes for the DTO
        List<UserRatingDto> changes = new ArrayList<>();

        // Helper to process a single user
        BiConsumer<User, Double> processPlayer = (user, opponentAvg) -> {
            int oldRating = user.getEloRating();
            boolean win = (winnerTeam.equals("A") && teamAPlayers.contains(user))
                    || (winnerTeam.equals("B") && teamBPlayers.contains(user));
            // Compute new rating
            int newRating = eloCalc.computeNewRating(user, opponentAvg, win);
            user.setEloRating(newRating);
            user.setGamesPlayed(user.getGamesPlayed() + 1);
            // Save RankHistory entry
            RankHistory history = new RankHistory();
            history.setUserId(user);
            history.setMatchId(match);
            history.setRatingBefore(oldRating);
            history.setRatingAfter(newRating);
            history.setTimestamp(new Date());
            rankHistoryRepo.save(history);
            // Add to DTO changes
            changes.add(new UserRatingDto(user.getId(), oldRating, newRating, user.getGamesPlayed()));
        };

        // Update all players on Team A and Team B
        for (User userA : teamAPlayers) {
            processPlayer.accept(userA, teamBAvg);
        }
        for (User userB : teamBPlayers) {
            processPlayer.accept(userB, teamAAvg);
        }

        // Persist updated user ratings in the database
        userRepo.saveAll(Stream.concat(teamAPlayers.stream(), teamBPlayers.stream()).toList());

        // Build and return the summary DTO
        MatchRatingUpdateDto resultDto = new MatchRatingUpdateDto(match.getId(), changes);
        return resultDto;
    }
}
