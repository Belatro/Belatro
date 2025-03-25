package backend.belatro.repos;

import backend.belatro.models.Lobbies;
import backend.belatro.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LobbiesRepo extends MongoRepository<Lobbies,String> {
}
