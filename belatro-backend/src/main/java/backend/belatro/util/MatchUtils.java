package backend.belatro.util;

import backend.belatro.dtos.MatchDTO;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatchUtils {
    private static final Pattern WINNER = Pattern.compile("^(Team [AB]) wins");

    /** "Team A" or "Team B", or null if unparsable */
    public static String parseWinner(String result) {
        if (result == null) return null;
        Matcher m = WINNER.matcher(result);
        return m.find() ? m.group(1) : null;
    }

    public static boolean isPlayerOnWinningTeam(MatchDTO m, String playerId) {

        // 1. Work out which side the player was on
        boolean onA = m.getTeamA().stream()
                .anyMatch(u -> u.getId().equals(playerId));
        boolean onB = m.getTeamB().stream()
                .anyMatch(u -> u.getId().equals(playerId));

        if (!onA && !onB) return false;          // shouldn’t happen

        // 2. Derive the winning team *robustly*
        String res = Optional.ofNullable(m.getResult()).orElse("");

        // Either the textual prefix (“Team A wins …”) …
        boolean teamAWon = res.startsWith("Team A");
        boolean teamBWon = res.startsWith("Team B");

        // … or (fallback) by comparing the two numbers, accepting both “-” and “–”
        if (!teamAWon && !teamBWon) {
            Matcher m2 = Pattern.compile("(\\d+)\\s*[–-]\\s*(\\d+)").matcher(res);
            if (m2.find()) {
                int a = Integer.parseInt(m2.group(1));
                int b = Integer.parseInt(m2.group(2));
                teamAWon = a > b;
                teamBWon = b > a;
            }
        }

        return (teamAWon && onA) || (teamBWon && onB);
    }


    public static boolean isPlayerOnLosingTeam(MatchDTO match, String playerId) {
        var w = parseWinner(match.getResult());
        if ("Team A".equals(w)) return match.getTeamB().contains(playerId);
        if ("Team B".equals(w)) return match.getTeamA().contains(playerId);
        return false;
    }
}
