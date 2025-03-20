package backend.belatro.repos;

import backend.belatro.models.MatchmakingQueue;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RankHistoryRepo extends MongoRepository<MatchmakingQueue,String> {

}
