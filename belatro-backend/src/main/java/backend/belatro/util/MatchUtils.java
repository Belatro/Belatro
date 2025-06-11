package backend.belatro.util;

import backend.belatro.dtos.MatchDTO;

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

    public static boolean isPlayerOnWinningTeam(MatchDTO match, String playerId) {
        var w = parseWinner(match.getResult());
        if ("Team A".equals(w)) return match.getTeamA().contains(playerId);
        if ("Team B".equals(w)) return match.getTeamB().contains(playerId);
        return false;
    }

    public static boolean isPlayerOnLosingTeam(MatchDTO match, String playerId) {
        var w = parseWinner(match.getResult());
        if ("Team A".equals(w)) return match.getTeamB().contains(playerId);
        if ("Team B".equals(w)) return match.getTeamA().contains(playerId);
        return false;
    }
}
