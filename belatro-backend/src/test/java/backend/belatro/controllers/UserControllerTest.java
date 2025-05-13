package backend.belatro.controllers;

import backend.belatro.dtos.UserUpdateDTO;
import backend.belatro.exceptions.UserNotFoundException;
import backend.belatro.models.User;
import backend.belatro.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserControllerTest {
    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private List<User> testUsers;
    private UserUpdateDTO testUpdateDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test data
        testUser = new User();
        testUser.setId("user123");
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");

        User user2 = new User();
        user2.setId("user456");
        user2.setUsername("anotherUser");
        user2.setEmail("another@example.com");

        testUsers = Arrays.asList(testUser, user2);

        testUpdateDTO = new UserUpdateDTO();
        testUpdateDTO.setUsername("updatedUsername");
        testUpdateDTO.setEmail("updated@example.com");
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Arrange
        when(userService.findAll()).thenReturn(testUsers);

        // Act
        ResponseEntity<List<User>> result = userController.getAllUsers();

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().size());
        verify(userService, times(1)).findAll();
    }

    @Test
    void getUserById_ShouldReturnUser() {
        // Arrange
        when(userService.findById("user123")).thenReturn(Optional.of(testUser));

        // Act
        ResponseEntity<User> result = userController.getUserById("user123");

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("user123", result.getBody().getId());
        assertEquals("testUser", result.getBody().getUsername());
        verify(userService, times(1)).findById("user123");
    }

    @Test
    void getUserById_WhenUserNotFound_ShouldThrowException() {
        // Arrange
        when(userService.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userController.getUserById("nonexistent");
        });
        verify(userService, times(1)).findById("nonexistent");
    }

    @Test
    void createUser_ShouldReturnCreatedUser() {
        // Arrange
        when(userService.createUser(any(User.class))).thenReturn(testUser);

        // Act
        ResponseEntity<User> result = userController.createUser(new User());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("user123", result.getBody().getId());
        assertEquals("testUser", result.getBody().getUsername());
        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    void updateUser_ShouldReturnUpdatedUser() {
        // Arrange
        User updatedUser = new User();
        updatedUser.setId("user123");
        updatedUser.setUsername("updatedUsername");
        updatedUser.setEmail("updated@example.com");

        when(userService.updateUser(anyString(), any(UserUpdateDTO.class))).thenReturn(updatedUser);

        // Act
        ResponseEntity<User> result = userController.updateUser("user123", testUpdateDTO);

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("user123", result.getBody().getId());
        assertEquals("updatedUsername", result.getBody().getUsername());
        assertEquals("updated@example.com", result.getBody().getEmail());
        verify(userService, times(1)).updateUser(anyString(), any(UserUpdateDTO.class));
    }

    @Test
    void deleteUser_ShouldReturnNoContent() {
        // Act
        ResponseEntity<Void> result = userController.deleteUser("user123");

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(userService, times(1)).deleteUser("user123");
    }
}