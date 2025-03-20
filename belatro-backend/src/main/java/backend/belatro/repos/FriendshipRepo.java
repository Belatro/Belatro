package backend.belatro.repos;

import backend.belatro.models.Friendship;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FriendshipRepo extends MongoRepository<Friendship,String> {
    List<Friendship> findByFromUser_IdOrToUser_Id(String userId, String userId1);
}
