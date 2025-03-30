package backend.belatro.pojo;

import backend.belatro.pojo.enums.Boja;
import backend.belatro.pojo.enums.Rank;
import lombok.Data;


@Data
public class Karta {
    private final Boja boja;
    private final Rank rank;

    public Karta(Boja boja, Rank rank) {
        this.boja = boja;
        this.rank = rank;
    }
    @Override
    public String toString() {
        return rank + " of " + boja;
    }

}
