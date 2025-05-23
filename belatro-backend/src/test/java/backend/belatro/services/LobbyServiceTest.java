package backend.belatro.services;

import backend.belatro.dtos.JoinLobbyRequestDTO;
import backend.belatro.dtos.LobbyDTO;
import backend.belatro.dtos.MatchDTO;
import backend.belatro.dtos.TeamSwitchRequestDTO;
import backend.belatro.enums.GameMode;
import backend.belatro.enums.lobbyStatus;
import backend.belatro.models.Lobbies;
import backend.belatro.models.User;
import backend.belatro.repos.LobbiesRepo;
import backend.belatro.repos.UserRepo;
import backend.belatro.services.impl.LobbyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LobbyServiceTest {
    @Mock
    private LobbiesRepo lobbyRepo;

    @Mock
    private UserRepo userRepo;

    @Mock
    private IMatchService matchService;

    @InjectMocks
    private LobbyServiceImpl lobbyService;

    private User testUser;
    private Lobbies testLobby;
    private LobbyDTO testLobbyDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test user
        testUser = new User();
        testUser.setId("user123");
        testUser.setUsername("testUser");

        // Setup test lobby
        testLobby = new Lobbies();
        testLobby.setId("lobby123");
        testLobby.setName("Test Lobby");
        testLobby.setGameMode("CASUAL");
        testLobby.setStatus(lobbyStatus.WAITING);
        testLobby.setHostUser(testUser);
        testLobby.setTeamAPlayers(new ArrayList<>());
        testLobby.getTeamAPlayers().add(testUser);
        testLobby.setTeamBPlayers(new ArrayList<>());
        testLobby.setUnassignedPlayers(new ArrayList<>());
        testLobby.setCreatedAt(new Date());
        testLobby.setPrivateLobby(false);

        // Setup test lobby DTO
        testLobbyDTO = new LobbyDTO();
        testLobbyDTO.setId("lobby123");
        testLobbyDTO.setName("Test Lobby");
        testLobbyDTO.setGameMode("CASUAL");
        testLobbyDTO.setStatus(lobbyStatus.WAITING);

        LobbyDTO.UserSimpleDTO hostDto = new LobbyDTO.UserSimpleDTO();
        hostDto.setId("user123");
        hostDto.setUsername("testUser");
        testLobbyDTO.setHostUser(hostDto);

        testLobbyDTO.setTeamAPlayers(new ArrayList<>());
        testLobbyDTO.getTeamAPlayers().add(hostDto);
        testLobbyDTO.setTeamBPlayers(new ArrayList<>());
        testLobbyDTO.setUnassignedPlayers(new ArrayList<>());
        testLobbyDTO.setCreatedAt(new Date());
        testLobbyDTO.setPrivateLobby(false);
    }

    // Tests for createLobby method
    @Test
    void createLobby_ShouldCreatePublicLobby() {
        // Arrange
        when(userRepo.findById(anyString())).thenReturn(Optional.of(testUser));
        when(lobbyRepo.save(any(Lobbies.class))).thenReturn(testLobby);

        // Act
        LobbyDTO result = lobbyService.createLobby(testLobbyDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Test Lobby", result.getName());
        assertEquals("CASUAL", result.getGameMode());
        assertEquals(lobbyStatus.WAITING, result.getStatus());
        assertEquals("user123", result.getHostUser().getId());
        assertEquals(1, result.getTeamAPlayers().size());
        assertEquals("user123", result.getTeamAPlayers().get(0).getId());
        assertFalse(result.isPrivateLobby());

        verify(userRepo, times(1)).findById("user123");
        verify(lobbyRepo, times(1)).save(any(Lobbies.class));
    }

    @Test
    void createLobby_ShouldCreatePrivateLobby() {
        // Arrange
        testLobbyDTO.setPrivateLobby(true);
        testLobbyDTO.setPassword("secret");

        testLobby.setPrivateLobby(true);
        testLobby.setPassword(new BCryptPasswordEncoder().encode("secret"));

        when(userRepo.findById(anyString())).thenReturn(Optional.of(testUser));
        when(lobbyRepo.save(any(Lobbies.class))).thenReturn(testLobby);

        // Act
        LobbyDTO result = lobbyService.createLobby(testLobbyDTO);

        // Assert
        assertNotNull(result);
        assertTrue(result.isPrivateLobby());
        assertNull(result.getPassword()); // Password should not be returned in DTO

        verify(userRepo, times(1)).findById("user123");
        verify(lobbyRepo, times(1)).save(any(Lobbies.class));
    }

    @Test
    void createLobby_WithoutHost_ShouldThrowException() {
        // Arrange
        testLobbyDTO.setHostUser(null);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.createLobby(testLobbyDTO);
        });

        assertEquals("Host user is required", exception.getMessage());
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    @Test
    void createLobby_PrivateWithoutPassword_ShouldThrowException() {
        // Arrange
        testLobbyDTO.setPrivateLobby(true);
        testLobbyDTO.setPassword("");

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.createLobby(testLobbyDTO);
        });

        assertEquals("Password required for private lobby", exception.getMessage());
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    @Test
    void createLobby_HostUserNotFound_ShouldThrowException() {
        // Arrange
        when(userRepo.findById(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.createLobby(testLobbyDTO);
        });

        assertEquals("Host user not found", exception.getMessage());
        verify(userRepo, times(1)).findById("user123");
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    // Tests for joinLobby method
    @Test
    void joinLobby_ShouldAddUserToUnassigned() {
        // Arrange
        User joiningUser = new User();
        joiningUser.setId("user456");
        joiningUser.setUsername("joiningUser");

        JoinLobbyRequestDTO joinRequest = new JoinLobbyRequestDTO();
        joinRequest.setLobbyId("lobby123");
        joinRequest.setUserId("user456");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(userRepo.findById("user456")).thenReturn(Optional.of(joiningUser));
        when(lobbyRepo.save(any(Lobbies.class))).thenReturn(testLobby);

        // Act
        LobbyDTO result = lobbyService.joinLobby(joinRequest);

        // Assert
        assertNotNull(result);
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(userRepo, times(1)).findById("user456");
        verify(lobbyRepo, times(1)).save(any(Lobbies.class));
    }

    @Test
    void joinLobby_PrivateLobbyWithCorrectPassword_ShouldJoin() {
        // Arrange
        testLobby.setPrivateLobby(true);
        testLobby.setPassword(new BCryptPasswordEncoder().encode("secret"));

        User joiningUser = new User();
        joiningUser.setId("user456");
        joiningUser.setUsername("joiningUser");

        JoinLobbyRequestDTO joinRequest = new JoinLobbyRequestDTO();
        joinRequest.setLobbyId("lobby123");
        joinRequest.setUserId("user456");
        joinRequest.setPassword("secret");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(userRepo.findById("user456")).thenReturn(Optional.of(joiningUser));
        when(lobbyRepo.save(any(Lobbies.class))).thenReturn(testLobby);

        // Act
        LobbyDTO result = lobbyService.joinLobby(joinRequest);

        // Assert
        assertNotNull(result);
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(userRepo, times(1)).findById("user456");
        verify(lobbyRepo, times(1)).save(any(Lobbies.class));
    }

    @Test
    void joinLobby_PrivateLobbyWithWrongPassword_ShouldThrowException() {
        // Arrange
        testLobby.setPrivateLobby(true);
        testLobby.setPassword(new BCryptPasswordEncoder().encode("secret"));

        JoinLobbyRequestDTO joinRequest = new JoinLobbyRequestDTO();
        joinRequest.setLobbyId("lobby123");
        joinRequest.setUserId("user456");
        joinRequest.setPassword("wrong");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.joinLobby(joinRequest);
        });

        assertEquals("Invalid lobby password", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    @Test
    void joinLobby_PrivateLobbyWithoutPassword_ShouldThrowException() {
        // Arrange
        testLobby.setPrivateLobby(true);
        testLobby.setPassword(new BCryptPasswordEncoder().encode("secret"));

        JoinLobbyRequestDTO joinRequest = new JoinLobbyRequestDTO();
        joinRequest.setLobbyId("lobby123");
        joinRequest.setUserId("user456");
        joinRequest.setPassword("");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.joinLobby(joinRequest);
        });

        assertEquals("Password required for private lobby", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    @Test
    void joinLobby_WhenLobbyIsFull_ShouldThrowException() {
        // Arrange
        User user2 = new User();
        user2.setId("user2");
        User user3 = new User();
        user3.setId("user3");
        User user4 = new User();
        user4.setId("user4");

        testLobby.getTeamAPlayers().add(user2);
        testLobby.getTeamBPlayers().add(user3);
        testLobby.getUnassignedPlayers().add(user4);

        JoinLobbyRequestDTO joinRequest = new JoinLobbyRequestDTO();
        joinRequest.setLobbyId("lobby123");
        joinRequest.setUserId("user5");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.joinLobby(joinRequest);
        });

        assertEquals("Lobby is full", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    @Test
    void joinLobby_UserAlreadyInLobby_ShouldNotAddAgain() {
        // Arrange
        JoinLobbyRequestDTO joinRequest = new JoinLobbyRequestDTO();
        joinRequest.setLobbyId("lobby123");
        joinRequest.setUserId("user123");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(userRepo.findById("user123")).thenReturn(Optional.of(testUser));

        // Act
        LobbyDTO result = lobbyService.joinLobby(joinRequest);

        // Assert
        assertNotNull(result);
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(userRepo, times(1)).findById("user123");
        // Should NOT save if user is already in the lobby
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }


    // Tests for getLobby method
    @Test
    void getLobby_ShouldReturnLobbyDTO() {
        // Arrange
        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));

        // Act
        LobbyDTO result = lobbyService.getLobby("lobby123");

        // Assert
        assertNotNull(result);
        assertEquals("lobby123", result.getId());
        assertEquals("Test Lobby", result.getName());
        assertEquals("CASUAL", result.getGameMode());
        assertEquals(lobbyStatus.WAITING, result.getStatus());
        assertEquals("user123", result.getHostUser().getId());
        verify(lobbyRepo, times(1)).findById("lobby123");
    }

    @Test
    void getLobby_WhenLobbyNotFound_ShouldThrowException() {
        // Arrange
        when(lobbyRepo.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.getLobby("nonexistent");
        });

        assertEquals("Lobby not found", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("nonexistent");
    }

    // Tests for updateLobby method
    @Test
    void updateLobby_ShouldUpdateAndReturnLobbyDTO() {
        // Arrange
        testLobbyDTO.setName("Updated Lobby Name");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(lobbyRepo.save(any(Lobbies.class))).thenReturn(testLobby);

        // Act
        LobbyDTO result = lobbyService.updateLobby(testLobbyDTO);

        // Assert
        assertNotNull(result);
        assertEquals("lobby123", result.getId());
        assertEquals("Updated Lobby Name", result.getName());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(lobbyRepo, times(1)).save(any(Lobbies.class));
    }

    @Test
    void updateLobby_WhenLobbyNotFound_ShouldThrowException() {
        // Arrange
        testLobbyDTO.setId("nonexistent");
        when(lobbyRepo.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.updateLobby(testLobbyDTO);
        });

        assertEquals("Lobby not found", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("nonexistent");
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    @Test
    void updateLobby_PrivateWithoutPassword_ShouldThrowException() {
        // Arrange
        testLobbyDTO.setPrivateLobby(true);
        testLobbyDTO.setPassword("");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.updateLobby(testLobbyDTO);
        });

        assertEquals("Password required for private lobby", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    @Test
    void updateLobby_MakePublic_ShouldRemovePassword() {
        // Arrange
        testLobby.setPrivateLobby(true);
        testLobby.setPassword("hashedPassword");

        testLobbyDTO.setPrivateLobby(false);

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(lobbyRepo.save(any(Lobbies.class))).thenReturn(testLobby);

        // Act
        LobbyDTO result = lobbyService.updateLobby(testLobbyDTO);

        // Assert
        assertNotNull(result);
        assertFalse(result.isPrivateLobby());
        assertNull(result.getPassword());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(lobbyRepo, times(1)).save(any(Lobbies.class));
    }

    // Test for deleteLobby method
    @Test
    void deleteLobby_ShouldCallRepositoryDelete() {
        // Act
        lobbyService.deleteLobby("lobby123");

        // Assert
        verify(lobbyRepo, times(1)).deleteById("lobby123");
    }

    // Tests for switchTeam method
    @Test
    void switchTeam_ShouldMoveUserBetweenTeams() {
        // Arrange
        TeamSwitchRequestDTO switchRequest = new TeamSwitchRequestDTO();
        switchRequest.setLobbyId("lobby123");
        switchRequest.setUserId("user123");
        switchRequest.setTargetTeam("B");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(userRepo.findById("user123")).thenReturn(Optional.of(testUser));
        when(lobbyRepo.save(any(Lobbies.class))).thenReturn(testLobby);

        // Act
        LobbyDTO result = lobbyService.switchTeam(switchRequest);

        // Assert
        assertNotNull(result);
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(userRepo, times(1)).findById("user123");
        verify(lobbyRepo, times(1)).save(any(Lobbies.class));
    }

    @Test
    void switchTeam_UserNotInLobby_ShouldThrowException() {
        // Arrange
        TeamSwitchRequestDTO switchRequest = new TeamSwitchRequestDTO();
        switchRequest.setLobbyId("lobby123");
        switchRequest.setUserId("nonexistent");
        switchRequest.setTargetTeam("B");

        User user = new User();
        user.setId("nonexistent");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(userRepo.findById("nonexistent")).thenReturn(Optional.of(user));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.switchTeam(switchRequest);
        });

        assertEquals("User is not in the lobby", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(userRepo, times(1)).findById("nonexistent");
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    @Test
    void switchTeam_WithoutTargetTeam_ShouldThrowException() {
        // Arrange
        TeamSwitchRequestDTO switchRequest = new TeamSwitchRequestDTO();
        switchRequest.setLobbyId("lobby123");
        switchRequest.setUserId("user123");
        switchRequest.setTargetTeam("");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(userRepo.findById("user123")).thenReturn(Optional.of(testUser));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.switchTeam(switchRequest);
        });

        assertEquals("Target team is required", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(userRepo, times(1)).findById("user123");
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    @Test
    void switchTeam_ToTeamA_WhenTeamAIsFull_ShouldThrowException() {
        // Arrange
        User user2 = new User();
        user2.setId("user2");
        testLobby.getTeamAPlayers().add(user2);

        User user3 = new User();
        user3.setId("user3");
        testLobby.getUnassignedPlayers().add(user3);

        TeamSwitchRequestDTO switchRequest = new TeamSwitchRequestDTO();
        switchRequest.setLobbyId("lobby123");
        switchRequest.setUserId("user3");
        switchRequest.setTargetTeam("A");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(userRepo.findById("user3")).thenReturn(Optional.of(user3));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.switchTeam(switchRequest);
        });

        assertEquals("Team A is full", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(userRepo, times(1)).findById("user3");
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    @Test
    void switchTeam_ToTeamB_WhenTeamBIsFull_ShouldThrowException() {
        // Arrange
        User user2 = new User();
        user2.setId("user2");
        User user3 = new User();
        user3.setId("user3");
        testLobby.getTeamBPlayers().add(user2);
        testLobby.getTeamBPlayers().add(user3);

        TeamSwitchRequestDTO switchRequest = new TeamSwitchRequestDTO();
        switchRequest.setLobbyId("lobby123");
        switchRequest.setUserId("user123");
        switchRequest.setTargetTeam("B");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(userRepo.findById("user123")).thenReturn(Optional.of(testUser));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.switchTeam(switchRequest);
        });

        assertEquals("Team B is full", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(userRepo, times(1)).findById("user123");
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    @Test
    void switchTeam_ToUnassigned_WhenUnassignedIsFull_ShouldThrowException() {
        // Arrange
        User user2 = new User();
        user2.setId("user2");
        User user3 = new User();
        user3.setId("user3");
        User user4 = new User();
        user4.setId("user4");
        User user5 = new User();
        user5.setId("user5");

        testLobby.getUnassignedPlayers().add(user2);
        testLobby.getUnassignedPlayers().add(user3);
        testLobby.getUnassignedPlayers().add(user4);
        testLobby.getUnassignedPlayers().add(user5);

        TeamSwitchRequestDTO switchRequest = new TeamSwitchRequestDTO();
        switchRequest.setLobbyId("lobby123");
        switchRequest.setUserId("user123");
        switchRequest.setTargetTeam("U");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(userRepo.findById("user123")).thenReturn(Optional.of(testUser));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.switchTeam(switchRequest);
        });

        assertEquals("Unassigned is full", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(userRepo, times(1)).findById("user123");
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    @Test
    void switchTeam_WithInvalidTargetTeam_ShouldThrowException() {
        // Arrange
        TeamSwitchRequestDTO switchRequest = new TeamSwitchRequestDTO();
        switchRequest.setLobbyId("lobby123");
        switchRequest.setUserId("user123");
        switchRequest.setTargetTeam("X"); // Invalid team

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(userRepo.findById("user123")).thenReturn(Optional.of(testUser));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.switchTeam(switchRequest);
        });

        assertEquals("Invalid target team. Use A, B, or U.", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(userRepo, times(1)).findById("user123");
        verify(lobbyRepo, never()).save(any(Lobbies.class));
    }

    // Tests for startMatch method
    @Test
    void startMatch_WithEnoughPlayers_ShouldCreateMatch() {
        // Arrange
        User user2 = new User();
        user2.setId("user2");
        user2.setUsername("user2");

        User user3 = new User();
        user3.setId("user3");
        user3.setUsername("user3");

        User user4 = new User();
        user4.setId("user4");
        user4.setUsername("user4");

        testLobby.getTeamAPlayers().add(user2);
        testLobby.getTeamBPlayers().add(user3);
        testLobby.getTeamBPlayers().add(user4);

        MatchDTO matchDTO = new MatchDTO();
        matchDTO.setId("match123");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(lobbyRepo.save(any(Lobbies.class))).thenReturn(testLobby);
        when(matchService.createMatch(any(MatchDTO.class))).thenReturn(matchDTO);

        // Act
        MatchDTO result = lobbyService.startMatch("lobby123");

        // Assert
        assertNotNull(result);
        assertEquals("match123", result.getId());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(lobbyRepo, times(1)).save(any(Lobbies.class));
        verify(matchService, times(1)).createMatch(any(MatchDTO.class));
    }

    @Test
    void startMatch_WithRankedGameMode_ShouldSetRankedInMatchDTO() {
        // Arrange
        User user2 = new User();
        user2.setId("user2");
        user2.setUsername("user2");

        User user3 = new User();
        user3.setId("user3");
        user3.setUsername("user3");

        User user4 = new User();
        user4.setId("user4");
        user4.setUsername("user4");

        testLobby.setGameMode("RANKED");
        testLobby.getTeamAPlayers().add(user2);
        testLobby.getTeamBPlayers().add(user3);
        testLobby.getTeamBPlayers().add(user4);

        MatchDTO matchDTO = new MatchDTO();
        matchDTO.setId("match123");
        matchDTO.setGameMode(GameMode.RANKED);

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));
        when(lobbyRepo.save(any(Lobbies.class))).thenReturn(testLobby);
        when(matchService.createMatch(any(MatchDTO.class))).thenReturn(matchDTO);

        // Act
        MatchDTO result = lobbyService.startMatch("lobby123");

        // Assert
        assertNotNull(result);
        assertEquals("match123", result.getId());
        assertEquals(GameMode.RANKED, result.getGameMode());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(lobbyRepo, times(1)).save(any(Lobbies.class));
        verify(matchService, times(1)).createMatch(any(MatchDTO.class));
    }

    @Test
    void startMatch_WithNotEnoughPlayers_ShouldThrowException() {
        // Arrange
        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.startMatch("lobby123");
        });

        assertEquals("Cannot start match: there must be at least 4 players across both teams", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("lobby123");
        verify(matchService, never()).createMatch(any(MatchDTO.class));
    }

    @Test
    void startMatch_WhenLobbyNotFound_ShouldThrowException() {
        // Arrange
        when(lobbyRepo.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            lobbyService.startMatch("nonexistent");
        });

        assertEquals("Lobby not found", exception.getMessage());
        verify(lobbyRepo, times(1)).findById("nonexistent");
        verify(matchService, never()).createMatch(any(MatchDTO.class));
    }

    // Tests for mapToDTO method (indirectly through other methods)
    @Test
    void mapToDTO_WithNullHostUser_ShouldHandleGracefully() {
        // Arrange
        testLobby.setHostUser(null);
        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));

        // Act
        LobbyDTO result = lobbyService.getLobby("lobby123");

        // Assert
        assertNotNull(result);
        assertNull(result.getHostUser());
        verify(lobbyRepo, times(1)).findById("lobby123");
    }

    @Test
    void mapToDTO_ShouldMapAllTeamPlayers() {
        // Arrange
        User user2 = new User();
        user2.setId("user2");
        user2.setUsername("user2");

        User user3 = new User();
        user3.setId("user3");
        user3.setUsername("user3");

        User user4 = new User();
        user4.setId("user4");
        user4.setUsername("user4");

        testLobby.getTeamAPlayers().add(user2);
        testLobby.getTeamBPlayers().add(user3);
        testLobby.getUnassignedPlayers().add(user4);

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));

        // Act
        LobbyDTO result = lobbyService.getLobby("lobby123");

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getTeamAPlayers().size());
        assertEquals(1, result.getTeamBPlayers().size());
        assertEquals(1, result.getUnassignedPlayers().size());

        // Check team A players
        List<String> teamAIds = result.getTeamAPlayers().stream()
                .map(LobbyDTO.UserSimpleDTO::getId)
                .collect(java.util.stream.Collectors.toList());
        assertTrue(teamAIds.contains("user123"));
        assertTrue(teamAIds.contains("user2"));

        // Check team B players
        assertEquals("user3", result.getTeamBPlayers().get(0).getId());

        // Check unassigned players
        assertEquals("user4", result.getUnassignedPlayers().get(0).getId());

        verify(lobbyRepo, times(1)).findById("lobby123");
    }

    @Test
    void mapToDTO_ShouldNotIncludePasswordInDTO() {
        // Arrange
        testLobby.setPrivateLobby(true);
        testLobby.setPassword("hashedPassword");

        when(lobbyRepo.findById("lobby123")).thenReturn(Optional.of(testLobby));

        // Act
        LobbyDTO result = lobbyService.getLobby("lobby123");

        // Assert
        assertNotNull(result);
        assertTrue(result.isPrivateLobby());
        assertNull(result.getPassword()); // Password should not be included in DTO
        verify(lobbyRepo, times(1)).findById("lobby123");
    }

    // Additional test for convertUserToUserSimple method
    @Test
    void convertUserToUserSimple_ShouldMapUserProperties() {
        // This is tested indirectly through other tests that use mapToDTO
        // But we can add a specific test if needed

        // Arrange
        User user = new User();
        user.setId("user123");
        user.setUsername("testUser");

        // Act - We need to use reflection to access this private method
        LobbyDTO.UserSimpleDTO result;
        try {
            java.lang.reflect.Method method = LobbyServiceImpl.class.getDeclaredMethod("convertUserToUserSimple", User.class);
            method.setAccessible(true);
            result = (LobbyDTO.UserSimpleDTO) method.invoke(lobbyService, user);

            // Assert
            assertNotNull(result);
            assertEquals("user123", result.getId());
            assertEquals("testUser", result.getUsername());
        } catch (Exception e) {
            fail("Exception occurred while testing convertUserToUserSimple: " + e.getMessage());
        }
    }
}


