package backend.belatro.services;

import backend.belatro.dtos.HandDTO;
import backend.belatro.dtos.MatchDTO;
import backend.belatro.dtos.MoveDTO;
import backend.belatro.enums.MoveType;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface IMatchService {
    MatchDTO createMatch(MatchDTO matchDTO);
    MatchDTO getMatch(String id);
    List<MatchDTO> getAllMatches();
    MatchDTO updateMatch(String id,
                         MatchDTO matchDTO);
    void deleteMatch(String id);
    void recordMove(String matchId,
                    MoveType type,
                    Map<String, Object> payload,
                    double evaluation);

    List<HandDTO> getStructuredMoves(String matchId);

    @Transactional
    void finaliseMatch(String matchId,
                       String winnerString,   // “Team A wins 1001–777”
                       Instant endTs);

    List<MoveDTO> getMoves(String matchId);
    MatchDTO getMatchByLobbyId(String lobbyId);



}
