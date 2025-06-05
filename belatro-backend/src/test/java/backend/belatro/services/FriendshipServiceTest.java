package backend.belatro.services;

import backend.belatro.dtos.CreateFriendshipDTO;
import backend.belatro.enums.FriendshipStatus;
import backend.belatro.exceptions.InvalidStateTransitionException;
import backend.belatro.exceptions.ResourceNotFoundException;
import backend.belatro.models.Friendship;
import backend.belatro.models.User;
import backend.belatro.repos.FriendshipRepo;
import backend.belatro.repos.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FriendshipServiceTest {

    @Mock
    private FriendshipRepo friendshipRepo;

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private FriendshipService friendshipService;

    private User testUser1;
    private User testUser2;
    private Friendship testFriendship;
    private CreateFriendshipDTO testCreateFriendshipDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test users
        testUser1 = new User();
        testUser1.setId("user1");
        testUser1.setUsername("testUser1");

        testUser2 = new User();
        testUser2.setId("user2");
        testUser2.setUsername("testUser2");

        // Setup test friendship
        testFriendship = new Friendship();
        testFriendship.setId("friendship123");
        testFriendship.setFromUser(testUser1);
        testFriendship.setToUser(testUser2);
        testFriendship.setStatus(FriendshipStatus.PENDING);
        testFriendship.setCreatedAt(new Date());

        // Setup test create friendship DTO
        testCreateFriendshipDTO = new CreateFriendshipDTO();
        testCreateFriendshipDTO.setFromUserId("user1");
        testCreateFriendshipDTO.setToUserId("user2");
        testCreateFriendshipDTO.setStatus(FriendshipStatus.PENDING);
    }

    @Test
    void getAllFriendships_ShouldReturnListOfFriendships() {
        // Arrange
        List<Friendship> friendships = new ArrayList<>();
        friendships.add(testFriendship);
        when(friendshipRepo.findAll()).thenReturn(friendships);

        // Act
        List<Friendship> result = friendshipService.getAllFriendships();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("friendship123", result.get(0).getId());
        verify(friendshipRepo, times(1)).findAll();
    }

    @Test
    void getFriendshipById_WhenFriendshipExists_ShouldReturnFriendship() {
        // Arrange
        when(friendshipRepo.findById("friendship123")).thenReturn(Optional.of(testFriendship));

        // Act
        Friendship result = friendshipService.getFriendshipById("friendship123");

        // Assert
        assertNotNull(result);
        assertEquals("friendship123", result.getId());
        assertEquals(testUser1, result.getFromUser());
        assertEquals(testUser2, result.getToUser());
        assertEquals(FriendshipStatus.PENDING, result.getStatus());
        verify(friendshipRepo, times(1)).findById("friendship123");
    }

    @Test
    void getFriendshipById_WhenFriendshipDoesNotExist_ShouldThrowException() {
        // Arrange
        when(friendshipRepo.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            friendshipService.getFriendshipById("nonexistent");
        });

        assertEquals("Friendship not found with id: nonexistent", exception.getMessage());
        verify(friendshipRepo, times(1)).findById("nonexistent");
    }

    @Test
    void getFriendshipsByUser_ShouldReturnListOfFriendships() {
        // Arrange
        List<Friendship> friendships = new ArrayList<>();
        friendships.add(testFriendship);
        when(friendshipRepo.findByFromUser_IdOrToUser_Id("user1", "user1")).thenReturn(friendships);

        // Act
        List<Friendship> result = friendshipService.getFriendshipsByUser("user1");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("friendship123", result.get(0).getId());
        verify(friendshipRepo, times(1)).findByFromUser_IdOrToUser_Id("user1", "user1");
    }

    @Test
    void createFriendship_ShouldCreateAndReturnFriendship() {
        // Arrange
        when(userRepo.findById("user1")).thenReturn(Optional.of(testUser1));
        when(userRepo.findById("user2")).thenReturn(Optional.of(testUser2));
        when(friendshipRepo.save(any(Friendship.class))).thenReturn(testFriendship);

        // Act
        Friendship result = friendshipService.createFriendship(testCreateFriendshipDTO);

        // Assert
        assertNotNull(result);
        assertEquals("friendship123", result.getId());
        assertEquals(testUser1, result.getFromUser());
        assertEquals(testUser2, result.getToUser());
        assertEquals(FriendshipStatus.PENDING, result.getStatus());
        verify(userRepo, times(1)).findById("user1");
        verify(userRepo, times(1)).findById("user2");
        verify(friendshipRepo, times(1)).save(any(Friendship.class));
    }

    @Test
    void createFriendship_WhenFromUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepo.findById("user1")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            friendshipService.createFriendship(testCreateFriendshipDTO);
        });

        assertEquals("User not found with id: user1", exception.getMessage());
        verify(userRepo, times(1)).findById("user1");
        verify(userRepo, never()).findById("user2");
        verify(friendshipRepo, never()).save(any(Friendship.class));
    }

    @Test
    void createFriendship_WhenToUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepo.findById("user1")).thenReturn(Optional.of(testUser1));
        when(userRepo.findById("user2")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            friendshipService.createFriendship(testCreateFriendshipDTO);
        });

        assertEquals("User not found with id: user2", exception.getMessage());
        verify(userRepo, times(1)).findById("user1");
        verify(userRepo, times(1)).findById("user2");
        verify(friendshipRepo, never()).save(any(Friendship.class));
    }

    @Test
    void updateFriendshipStatus_FromPendingToAccepted_ShouldUpdateStatus() {
        // Arrange
        when(friendshipRepo.findById("friendship123")).thenReturn(Optional.of(testFriendship));
        when(friendshipRepo.save(any(Friendship.class))).thenReturn(testFriendship);

        // Act
        Friendship result = friendshipService.updateFriendshipStatus("friendship123", FriendshipStatus.ACCEPTED);

        // Assert
        assertNotNull(result);
        assertEquals(FriendshipStatus.ACCEPTED, result.getStatus());
        verify(friendshipRepo, times(1)).findById("friendship123");
        verify(friendshipRepo, times(1)).save(any(Friendship.class));
    }

    @Test
    void updateFriendshipStatus_FromPendingToRejected_ShouldUpdateStatus() {
        // Arrange
        when(friendshipRepo.findById("friendship123")).thenReturn(Optional.of(testFriendship));
        when(friendshipRepo.save(any(Friendship.class))).thenReturn(testFriendship);

        // Act
        Friendship result = friendshipService.updateFriendshipStatus("friendship123", FriendshipStatus.REJECTED);

        // Assert
        assertNotNull(result);
        assertEquals(FriendshipStatus.REJECTED, result.getStatus());
        verify(friendshipRepo, times(1)).findById("friendship123");
        verify(friendshipRepo, times(1)).save(any(Friendship.class));
    }

    @Test
    void updateFriendshipStatus_FromPendingToCancelled_ShouldUpdateStatus() {
        // Arrange
        when(friendshipRepo.findById("friendship123")).thenReturn(Optional.of(testFriendship));
        when(friendshipRepo.save(any(Friendship.class))).thenReturn(testFriendship);

        // Act
        Friendship result = friendshipService.updateFriendshipStatus("friendship123", FriendshipStatus.CANCELLED);

        // Assert
        assertNotNull(result);
        assertEquals(FriendshipStatus.CANCELLED, result.getStatus());
        verify(friendshipRepo, times(1)).findById("friendship123");
        verify(friendshipRepo, times(1)).save(any(Friendship.class));
    }

    @Test
    void updateFriendshipStatus_FromAcceptedToRejected_ShouldThrowException() {
        // Arrange
        testFriendship.setStatus(FriendshipStatus.ACCEPTED);
        when(friendshipRepo.findById("friendship123")).thenReturn(Optional.of(testFriendship));

        // Act & Assert
        Exception exception = assertThrows(InvalidStateTransitionException.class, () -> {
            friendshipService.updateFriendshipStatus("friendship123", FriendshipStatus.REJECTED);
        });

        assertEquals("Cannot change status once friendship is finalized.", exception.getMessage());
        verify(friendshipRepo, times(1)).findById("friendship123");
        verify(friendshipRepo, never()).save(any(Friendship.class));
    }

    @Test
    void updateFriendshipStatus_FromRejectedToAccepted_ShouldThrowException() {
        // Arrange
        testFriendship.setStatus(FriendshipStatus.REJECTED);
        when(friendshipRepo.findById("friendship123")).thenReturn(Optional.of(testFriendship));

        // Act & Assert
        Exception exception = assertThrows(InvalidStateTransitionException.class, () -> {
            friendshipService.updateFriendshipStatus("friendship123", FriendshipStatus.ACCEPTED);
        });

        assertEquals("Cannot change status once friendship is finalized.", exception.getMessage());
        verify(friendshipRepo, times(1)).findById("friendship123");
        verify(friendshipRepo, never()).save(any(Friendship.class));
    }

    @Test
    void updateFriendshipStatus_FromCancelledToPending_ShouldThrowException() {
        // Arrange
        testFriendship.setStatus(FriendshipStatus.CANCELLED);
        when(friendshipRepo.findById("friendship123")).thenReturn(Optional.of(testFriendship));

        // Act & Assert
        Exception exception = assertThrows(InvalidStateTransitionException.class, () -> {
            friendshipService.updateFriendshipStatus("friendship123", FriendshipStatus.PENDING);
        });

        assertEquals("Cannot change status once friendship is finalized.", exception.getMessage());
        verify(friendshipRepo, times(1)).findById("friendship123");
        verify(friendshipRepo, never()).save(any(Friendship.class));
    }

    @Test
    void updateFriendshipStatus_WhenFriendshipNotFound_ShouldThrowException() {
        // Arrange
        when(friendshipRepo.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            friendshipService.updateFriendshipStatus("nonexistent", FriendshipStatus.ACCEPTED);
        });

        assertEquals("Friendship not found with id: nonexistent", exception.getMessage());
        verify(friendshipRepo, times(1)).findById("nonexistent");
        verify(friendshipRepo, never()).save(any(Friendship.class));
    }

    @Test
    void deleteFriendship_WhenFriendshipExists_ShouldDeleteFriendship() {
        // Arrange
        when(friendshipRepo.findById("friendship123")).thenReturn(Optional.of(testFriendship));
        doNothing().when(friendshipRepo).delete(any(Friendship.class));

        // Act
        friendshipService.deleteFriendship("friendship123");

        // Assert
        verify(friendshipRepo, times(1)).findById("friendship123");
        verify(friendshipRepo, times(1)).delete(any(Friendship.class));
    }

    @Test
    void deleteFriendship_WhenFriendshipNotFound_ShouldThrowException() {
        // Arrange
        when(friendshipRepo.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            friendshipService.deleteFriendship("nonexistent");
        });

        assertEquals("Friendship not found with id: nonexistent", exception.getMessage());
        verify(friendshipRepo, times(1)).findById("nonexistent");
        verify(friendshipRepo, never()).delete(any(Friendship.class));
    }
}
