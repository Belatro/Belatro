package backend.belatro.repos;

import backend.belatro.enums.lobbyStatus;
import backend.belatro.models.Lobbies;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LobbiesRepo extends MongoRepository<Lobbies,String> {
    List<Lobbies> findAllByStatus(lobbyStatus lobbyStatus);
}
