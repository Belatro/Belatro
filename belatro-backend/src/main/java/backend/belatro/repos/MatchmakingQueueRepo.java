package backend.belatro.repos;

import backend.belatro.models.MatchmakingQueue;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchmakingQueueRepo extends MongoRepository<MatchmakingQueue,String> {
}
