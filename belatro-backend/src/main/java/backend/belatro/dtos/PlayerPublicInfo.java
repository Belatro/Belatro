package backend.belatro.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PlayerPublicInfo {
    private String username;
    private String id;
    private int    cardsLeft;
}