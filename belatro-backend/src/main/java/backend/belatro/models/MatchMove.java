package backend.belatro.models;

import backend.belatro.enums.MoveType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "match_moves")
@Data @NoArgsConstructor @AllArgsConstructor
@Builder
public class MatchMove {

    @Id
    private String  id;
    private String  matchId;
    private int     number;
    private MoveType type;
    private Map<String, Object> payload;
    private double  evaluation;
    private Instant ts;


}
