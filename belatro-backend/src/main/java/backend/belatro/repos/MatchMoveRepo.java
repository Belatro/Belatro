package backend.belatro.repos;

import backend.belatro.enums.MoveType;
import backend.belatro.models.MatchMove;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchMoveRepo extends MongoRepository<MatchMove, String> {

    List<MatchMove> findByMatchIdOrderByNumber(String matchId);

    long countByMatchId(String matchId);
    void deleteByMatchId(String matchId);

    long countByMatchIdAndType(String matchId, MoveType moveType);

    List<MatchMove> findByMatchIdAndTypeOrderByNumberDesc(String matchId, MoveType moveType);

    Optional<MatchMove> findFirstByMatchIdAndTypeOrderByNumberDesc(String matchId, MoveType moveType);
}
