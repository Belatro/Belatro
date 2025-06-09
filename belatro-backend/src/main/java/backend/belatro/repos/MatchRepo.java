package backend.belatro.repos;

import backend.belatro.models.Match;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MatchRepo extends MongoRepository<Match,String> {
    Optional<Match> findByOriginLobbyId(String originLobbyId);

}
