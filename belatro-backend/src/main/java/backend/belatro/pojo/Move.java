package backend.belatro.pojo;

import lombok.Data;

import java.util.Date;


@Data
public class Move {
    private int moveNumber;
    private String moveData;
    private Date timestamp;
    private double evaluation;
}
