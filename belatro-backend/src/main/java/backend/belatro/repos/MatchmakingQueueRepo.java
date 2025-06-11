package backend.belatro.repos;

import backend.belatro.models.MatchmakingQueueEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchmakingQueueRepo extends MongoRepository<MatchmakingQueueEntry,String> {
    Optional<MatchmakingQueueEntry> findByUserIdAndStatus(String userId,
                                                          MatchmakingQueueEntry.Status status);

    List<MatchmakingQueueEntry> findByStatus(MatchmakingQueueEntry.Status status);

    List<MatchmakingQueueEntry>
    findAllByStatusOrderByEloSnapshotAsc(MatchmakingQueueEntry.Status status);
}
