package backend.belatro.repos;

import backend.belatro.models.Match;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchRepo extends MongoRepository<Match,String> {
}
