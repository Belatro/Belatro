package backend.belatro.controllers;

import backend.belatro.dtos.CreateFriendshipDTO;
import backend.belatro.enums.FriendshipStatus;
import backend.belatro.models.Friendship;
import backend.belatro.models.User;
import backend.belatro.services.FriendshipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
public class FriendshipControllerTest {

    @Mock
    private FriendshipService friendshipService;

    @InjectMocks
    private FriendshipController friendshipController;

    private Friendship testFriendship;
    private List<Friendship> testFriendships;
    private CreateFriendshipDTO testCreateDTO;
    private User fromUser;
    private User toUser;

    @BeforeEach
    void setUp() {
        // Setup test users
        fromUser = new User();
        fromUser.setId("user1");

        toUser = new User();
        toUser.setId("user2");

        // Setup test friendship
        testFriendship = new Friendship();
        testFriendship.setId("friendship123");
        testFriendship.setFromUser(fromUser);
        testFriendship.setToUser(toUser);
        testFriendship.setStatus(FriendshipStatus.PENDING);
        testFriendship.setCreatedAt(new Date());

        // Setup second test friendship
        User toUser2 = new User();
        toUser2.setId("user3");

        Friendship friendship2 = new Friendship();
        friendship2.setId("friendship456");
        friendship2.setFromUser(fromUser);
        friendship2.setToUser(toUser2);
        friendship2.setStatus(FriendshipStatus.ACCEPTED);
        friendship2.setCreatedAt(new Date());

        testFriendships = Arrays.asList(testFriendship, friendship2);

        // Setup test DTO
        testCreateDTO = new CreateFriendshipDTO();
        testCreateDTO.setFromUserId("user1");
        testCreateDTO.setToUserId("user2");
        testCreateDTO.setStatus(FriendshipStatus.PENDING);
    }

    @Test
    void getAllFriendships_ShouldReturnAllFriendships() {
        // Arrange
        when(friendshipService.getAllFriendships()).thenReturn(testFriendships);

        // Act
        ResponseEntity<List<Friendship>> response = friendshipController.getAllFriendships();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(friendshipService, times(1)).getAllFriendships();
    }

    @Test
    void getFriendshipById_ShouldReturnFriendship() {
        // Arrange
        when(friendshipService.getFriendshipById("friendship123")).thenReturn(testFriendship);

        // Act
        ResponseEntity<Friendship> response = friendshipController.getFriendshipById("friendship123");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("friendship123", response.getBody().getId());
        assertEquals(fromUser, response.getBody().getFromUser());
        assertEquals(toUser, response.getBody().getToUser());
        assertEquals(FriendshipStatus.PENDING, response.getBody().getStatus());
        verify(friendshipService, times(1)).getFriendshipById("friendship123");
    }

    @Test
    void getFriendshipsByUser_ShouldReturnUserFriendships() {
        // Arrange
        when(friendshipService.getFriendshipsByUser("user1")).thenReturn(testFriendships);

        // Act
        ResponseEntity<List<Friendship>> response = friendshipController.getFriendshipsByUser("user1");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
        verify(friendshipService, times(1)).getFriendshipsByUser("user1");
    }

    @Test
    void createFriendship_ShouldReturnCreatedFriendship() {
        // Arrange
        when(friendshipService.createFriendship(any(CreateFriendshipDTO.class))).thenReturn(testFriendship);

        // Act
        ResponseEntity<Friendship> response = friendshipController.createFriendship(testCreateDTO);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("friendship123", response.getBody().getId());
        assertEquals(FriendshipStatus.PENDING, response.getBody().getStatus());
        verify(friendshipService, times(1)).createFriendship(any(CreateFriendshipDTO.class));
    }

    @Test
    void acceptFriendship_ShouldReturnAcceptedFriendship() {
        // Arrange
        Friendship acceptedFriendship = new Friendship();
        acceptedFriendship.setId("friendship123");
        acceptedFriendship.setFromUser(fromUser);
        acceptedFriendship.setToUser(toUser);
        acceptedFriendship.setStatus(FriendshipStatus.ACCEPTED);

        when(friendshipService.updateFriendshipStatus("friendship123", FriendshipStatus.ACCEPTED))
                .thenReturn(acceptedFriendship);

        // Act
        ResponseEntity<Friendship> response = friendshipController.acceptFriendship("friendship123");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(FriendshipStatus.ACCEPTED, response.getBody().getStatus());
        verify(friendshipService, times(1))
                .updateFriendshipStatus("friendship123", FriendshipStatus.ACCEPTED);
    }

    @Test
    void rejectFriendship_ShouldReturnRejectedFriendship() {
        // Arrange
        Friendship rejectedFriendship = new Friendship();
        rejectedFriendship.setId("friendship123");
        rejectedFriendship.setFromUser(fromUser);
        rejectedFriendship.setToUser(toUser);
        rejectedFriendship.setStatus(FriendshipStatus.REJECTED);

        when(friendshipService.updateFriendshipStatus("friendship123", FriendshipStatus.REJECTED))
                .thenReturn(rejectedFriendship);

        // Act
        ResponseEntity<Friendship> response = friendshipController.rejectFriendship("friendship123");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(FriendshipStatus.REJECTED, response.getBody().getStatus());
        verify(friendshipService, times(1))
                .updateFriendshipStatus("friendship123", FriendshipStatus.REJECTED);
    }

    @Test
    void cancelFriendship_ShouldReturnCancelledFriendship() {
        // Arrange
        Friendship cancelledFriendship = new Friendship();
        cancelledFriendship.setId("friendship123");
        cancelledFriendship.setFromUser(fromUser);
        cancelledFriendship.setToUser(toUser);
        cancelledFriendship.setStatus(FriendshipStatus.CANCELLED);

        when(friendshipService.updateFriendshipStatus("friendship123", FriendshipStatus.CANCELLED))
                .thenReturn(cancelledFriendship);

        // Act
        ResponseEntity<Friendship> response = friendshipController.cancelFriendship("friendship123");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(FriendshipStatus.CANCELLED, response.getBody().getStatus());
        verify(friendshipService, times(1))
                .updateFriendshipStatus("friendship123", FriendshipStatus.CANCELLED);
    }

    @Test
    void deleteFriendship_ShouldReturnNoContent() {
        // Arrange
        doNothing().when(friendshipService).deleteFriendship(anyString());

        // Act
        ResponseEntity<Void> response = friendshipController.deleteFriendship("friendship123");

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(friendshipService, times(1)).deleteFriendship("friendship123");
    }
}
