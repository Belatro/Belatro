package backend.belatro.repos;

import backend.belatro.models.MatchmakingQueue;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RankHistoryRepo extends MongoRepository<MatchmakingQueue,String> {

}
