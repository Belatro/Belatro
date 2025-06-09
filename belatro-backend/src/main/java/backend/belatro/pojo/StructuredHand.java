package backend.belatro.pojo;

import backend.belatro.dtos.ChallengeDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StructuredHand {
    private final List<ChallengeDTO> challenges = new ArrayList<>();
    public List<ChallengeDTO> getChallenges() { return challenges; }
}
