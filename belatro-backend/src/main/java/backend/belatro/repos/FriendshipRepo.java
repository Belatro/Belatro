package backend.belatro.repos;

import backend.belatro.models.Friendship;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendshipRepo extends MongoRepository<Friendship,String> {
}
