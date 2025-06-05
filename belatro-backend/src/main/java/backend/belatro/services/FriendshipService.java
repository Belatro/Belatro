package backend.belatro.services;

import backend.belatro.dtos.CreateFriendshipDTO;
import backend.belatro.enums.FriendshipStatus;
import backend.belatro.exceptions.InvalidStateTransitionException;
import backend.belatro.exceptions.ResourceNotFoundException;
import backend.belatro.models.Friendship;
import backend.belatro.models.User;
import backend.belatro.repos.FriendshipRepo;
import backend.belatro.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class FriendshipService {

    private final FriendshipRepo friendshipRepo;
    private final UserRepo userRepo;

    @Autowired
    public FriendshipService(FriendshipRepo friendshipRepo, UserRepo userRepo) {
        this.friendshipRepo = friendshipRepo;
        this.userRepo = userRepo;
    }


    public List<Friendship> getAllFriendships() {
        return friendshipRepo.findAll();
    }


    public Friendship getFriendshipById(String id) {
        return friendshipRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship not found with id: " + id));
    }

    public List<Friendship> getFriendshipsByUser(String userId) {
        return friendshipRepo.findByFromUser_IdOrToUser_Id(userId, userId);
    }

    @Transactional
    public Friendship createFriendship(CreateFriendshipDTO dto) {
        User fromUser = userRepo.findById(dto.getFromUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getFromUserId()));
        User toUser = userRepo.findById(dto.getToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getToUserId()));

        Friendship friendship = new Friendship();
        friendship.setFromUser(fromUser);
        friendship.setToUser(toUser);
        friendship.setStatus(FriendshipStatus.PENDING);
        friendship.setCreatedAt(new Date());

        return friendshipRepo.save(friendship);
    }
    @Transactional
    public Friendship updateFriendshipStatus(String friendshipId, FriendshipStatus newStatus) {
        Friendship friendship = friendshipRepo.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship not found with id: " + friendshipId));

        if (friendship.getStatus() == FriendshipStatus.ACCEPTED ||
                friendship.getStatus() == FriendshipStatus.REJECTED ||
                friendship.getStatus() == FriendshipStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Cannot change status once friendship is finalized.");
        }

        friendship.setStatus(newStatus);
        return friendshipRepo.save(friendship);
    }



    @Transactional
    public void deleteFriendship(String friendshipId) {
        Friendship friendship = friendshipRepo.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Friendship not found with id: " + friendshipId));
        friendshipRepo.delete(friendship);
    }
}
