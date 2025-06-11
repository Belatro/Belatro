package backend.belatro.models;

import backend.belatro.enums.QueueState;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "matchmakingQueue")
public class MatchmakingQueue {

    @Id
    private String id;

    @DBRef
    private User user;

    private int eloRating;

    private Date queuedAt;

    private QueueState status;
}