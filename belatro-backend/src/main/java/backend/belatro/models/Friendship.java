package backend.belatro.models;

import backend.belatro.enums.FriendshipStatus;
import jdk.jshell.Snippet;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "friendships")
public class Friendship {

    @Id
    private String id;

    @DBRef
    private User fromUser;

    @DBRef
    private User toUser;

    private FriendshipStatus status;

    private Date createdAt;
}