package backend.belatro.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "matchmakingQueue")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchmakingQueueEntry {

    @Id
    private String   id;

    @Indexed(unique = true)
    private String   userId;           // fast lookup & "only once in queue"

    private int      eloSnapshot;      // rating when queued

    @Indexed(expireAfterSeconds = 600)
    private Instant queuedAt;         // auto-expires after 10 min (safety)


    private Status   status;           // QUEUED, MATCHED, CANCELLED

    @Version
    private Long     version;   // optimistic locking

    public enum Status { QUEUED, MATCHED, CANCELLED }
}
