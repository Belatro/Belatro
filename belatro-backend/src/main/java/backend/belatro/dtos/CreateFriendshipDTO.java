package backend.belatro.dtos;

import backend.belatro.enums.FriendshipStatus;
import lombok.Data;

@Data
public class CreateFriendshipDTO {
    private String fromUserId;
    private String toUserId;
    private FriendshipStatus status;
}