package backend.belatro.repos;

import backend.belatro.models.MatchMove;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchMoveRepo extends MongoRepository<MatchMove, String> {

    List<MatchMove> findByMatchIdOrderByNumber(String matchId);

    long countByMatchId(String matchId);
    void deleteByMatchId(String matchId);
}

