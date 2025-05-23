package backend.belatro.pojo.gamelogic;

import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.Rank;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@EqualsAndHashCode
@NoArgsConstructor(force = true)        // NEW
@JsonIgnoreProperties(ignoreUnknown = true)
public class Card {                     // idem for Bid, Trick, Player, …
    private Boja boja;
    private Rank rank;

    @JsonCreator                         // Optional if you keep the ctor
    public Card(@JsonProperty("boja") Boja boja,
                @JsonProperty("rank") Rank rank) {
        this.boja = boja;
        this.rank = rank;
    }
    @Override
    public String toString() {
        return rank + " of " + boja;
    }

}
