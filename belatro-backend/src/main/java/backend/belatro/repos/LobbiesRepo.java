package backend.belatro.repos;

import backend.belatro.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface LobbiesRepo extends MongoRepository<User,String> {
}
