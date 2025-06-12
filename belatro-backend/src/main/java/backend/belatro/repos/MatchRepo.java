package backend.belatro.repos;

import backend.belatro.models.Match;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchRepo extends MongoRepository<Match,String> {
    Optional<Match> findByOriginLobbyId(String originLobbyId);
    @Query("{ $or: [ { 'TeamA.$id': ?0 }, { 'TeamB.$id': ?0 } ] }")
    Page<Match> findByPlayerIdInTeams(String playerId, Pageable pageable);
    /**
     * Find all matches where `result` exists (i.e. finished),
     * sorted however the passed‐in Pageable says.
     */
    Page<Match> findByResultIsNotNull(Pageable pageable);
}
