package backend.belatro.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collation = "rankhistories")
public class RankHistory {
    @Id
    private String id;
    @DBRef
    private User userId;
    @DBRef
    private Match matchId;
    private int ratingBefore;
    private int ratingAfter;
    private Date timestamp;
}
