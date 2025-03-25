package backend.belatro.services;

import backend.belatro.dtos.MatchDTO;
import java.util.List;

public interface IMatchService {
    MatchDTO createMatch(MatchDTO matchDTO);
    MatchDTO getMatch(String id);
    List<MatchDTO> getAllMatches();
    MatchDTO updateMatch(String id, MatchDTO matchDTO);
    void deleteMatch(String id);
}
