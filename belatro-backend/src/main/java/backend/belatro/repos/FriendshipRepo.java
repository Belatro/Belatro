package backend.belatro.repos;

import backend.belatro.models.Friendship;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FriendshipRepo extends MongoRepository<Friendship,String> {
}
