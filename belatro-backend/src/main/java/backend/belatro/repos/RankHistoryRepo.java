package backend.belatro.repos;

import backend.belatro.models.RankHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RankHistoryRepo extends MongoRepository<RankHistory,String> {

}
