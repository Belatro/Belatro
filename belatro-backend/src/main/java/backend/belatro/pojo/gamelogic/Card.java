package backend.belatro.pojo.gamelogic;

import backend.belatro.pojo.gamelogic.enums.Boja;
import backend.belatro.pojo.gamelogic.enums.Rank;
import lombok.Data;


@Data
public class Card {
    private final Boja boja;
    private final Rank rank;

    public Card(Boja boja, Rank rank) {
        this.boja = boja;
        this.rank = rank;
    }
    @Override
    public String toString() {
        return rank + " of " + boja;
    }

}
