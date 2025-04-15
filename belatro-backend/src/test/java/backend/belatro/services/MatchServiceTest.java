package backend.belatro.services;

import backend.belatro.dtos.LobbyDTO;
import backend.belatro.dtos.MatchDTO;
import backend.belatro.enums.GameMode;
import backend.belatro.models.Lobbies;
import backend.belatro.models.Match;
import backend.belatro.models.User;
import backend.belatro.repos.MatchRepo;
import backend.belatro.services.impl.MatchServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MatchServiceImplTest {

    @Mock
    private MatchRepo matchRepo;

    @InjectMocks
    private MatchServiceImpl matchService;

    private Match testMatch;
    private MatchDTO testMatchDTO;
    private User testUser1;
    private User testUser2;
    private Lobbies testLobby;

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

        // Setup test lobby
        testLobby = new Lobbies();
        testLobby.setId("lobby123");
        testLobby.setName("Test Lobby");

        // Setup test match
        testMatch = new Match();
        testMatch.setId("match123");
        testMatch.setGameMode(GameMode.CASUAL);
        testMatch.setStartTime(new Date());
        testMatch.setTeamA(Arrays.asList(testUser1));
        testMatch.setTeamB(Arrays.asList(testUser2));
        testMatch.setOriginLobby(testLobby);
        testMatch.setMoves(new ArrayList<>());
        testMatch.setResult("Team A wins");

        // Setup test match DTO
        testMatchDTO = new MatchDTO();
        testMatchDTO.setId("match123");
        testMatchDTO.setGameMode(GameMode.CASUAL);
        testMatchDTO.setStartTime(new Date());

        List<LobbyDTO.UserSimpleDTO> teamA = new ArrayList<>();
        LobbyDTO.UserSimpleDTO user1Dto = new LobbyDTO.UserSimpleDTO();
        user1Dto.setId("user1");
        user1Dto.setUsername("testUser1");
        teamA.add(user1Dto);
        testMatchDTO.setTeamA(teamA);

        List<LobbyDTO.UserSimpleDTO> teamB = new ArrayList<>();
        LobbyDTO.UserSimpleDTO user2Dto = new LobbyDTO.UserSimpleDTO();
        user2Dto.setId("user2");
        user2Dto.setUsername("testUser2");
        teamB.add(user2Dto);
        testMatchDTO.setTeamB(teamB);

        LobbyDTO lobbyDTO = new LobbyDTO();
        lobbyDTO.setId("lobby123");
        lobbyDTO.setName("Test Lobby");
        testMatchDTO.setOriginLobby(lobbyDTO);

        testMatchDTO.setMoves(new ArrayList<>());
        testMatchDTO.setResult("Team A wins");
    }

    @Test
    void createMatch_ShouldSaveAndReturnMatchDTO() {
        // Arrange
        when(matchRepo.save(any(Match.class))).thenReturn(testMatch);

        // Act
        MatchDTO result = matchService.createMatch(testMatchDTO);

        // Assert
        assertNotNull(result);
        assertEquals("match123", result.getId());
        assertEquals(GameMode.CASUAL, result.getGameMode());
        assertEquals("Team A wins", result.getResult());
        assertEquals(1, result.getTeamA().size());
        assertEquals("user1", result.getTeamA().get(0).getId());
        assertEquals(1, result.getTeamB().size());
        assertEquals("user2", result.getTeamB().get(0).getId());
        assertEquals("lobby123", result.getOriginLobby().getId());

        verify(matchRepo, times(1)).save(any(Match.class));
    }

    @Test
    void getMatch_WhenMatchExists_ShouldReturnMatchDTO() {
        // Arrange
        when(matchRepo.findById("match123")).thenReturn(Optional.of(testMatch));

        // Act
        MatchDTO result = matchService.getMatch("match123");

        // Assert
        assertNotNull(result);
        assertEquals("match123", result.getId());
        assertEquals(GameMode.CASUAL, result.getGameMode());
        assertEquals("Team A wins", result.getResult());

        verify(matchRepo, times(1)).findById("match123");
    }

    @Test
    void getMatch_WhenMatchDoesNotExist_ShouldReturnNull() {
        // Arrange
        when(matchRepo.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        MatchDTO result = matchService.getMatch("nonexistent");

        // Assert
        assertNull(result);
        verify(matchRepo, times(1)).findById("nonexistent");
    }

    @Test
    void getAllMatches_ShouldReturnListOfMatchDTOs() {
        // Arrange
        when(matchRepo.findAll()).thenReturn(Arrays.asList(testMatch));

        // Act
        List<MatchDTO> results = matchService.getAllMatches();

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("match123", results.get(0).getId());

        verify(matchRepo, times(1)).findAll();
    }

    @Test
    void getAllMatches_WhenNoMatches_ShouldReturnEmptyList() {
        // Arrange
        when(matchRepo.findAll()).thenReturn(new ArrayList<>());

        // Act
        List<MatchDTO> results = matchService.getAllMatches();

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(matchRepo, times(1)).findAll();
    }

    @Test
    void updateMatch_WhenMatchExists_ShouldUpdateAndReturnMatchDTO() {
        // Arrange
        testMatchDTO.setResult("Team B wins");

        when(matchRepo.findById("match123")).thenReturn(Optional.of(testMatch));
        when(matchRepo.save(any(Match.class))).thenReturn(testMatch);

        // Act
        MatchDTO result = matchService.updateMatch("match123", testMatchDTO);

        // Assert
        assertNotNull(result);
        assertEquals("match123", result.getId());

        verify(matchRepo, times(1)).findById("match123");
        verify(matchRepo, times(1)).save(any(Match.class));
    }

    @Test
    void updateMatch_WhenMatchDoesNotExist_ShouldThrowException() {
        // Arrange
        when(matchRepo.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            matchService.updateMatch("nonexistent", testMatchDTO);
        });

        assertEquals("Match not found with id: nonexistent", exception.getMessage());
        verify(matchRepo, times(1)).findById("nonexistent");
        verify(matchRepo, never()).save(any(Match.class));
    }

    @Test
    void deleteMatch_ShouldCallRepositoryDelete() {
        // Act
        matchService.deleteMatch("match123");

        // Assert
        verify(matchRepo, times(1)).deleteById("match123");
    }

    @Test
    void toEntity_WithNullTeams_ShouldHandleGracefully() {
        // Arrange
        testMatchDTO.setTeamA(null);
        testMatchDTO.setTeamB(null);

        when(matchRepo.save(any(Match.class))).thenReturn(testMatch);

        // Act
        MatchDTO result = matchService.createMatch(testMatchDTO);

        // Assert
        assertNotNull(result);
        verify(matchRepo, times(1)).save(any(Match.class));
    }

    @Test
    void toEntity_WithNullOriginLobby_ShouldHandleGracefully() {
        // Arrange
        testMatchDTO.setOriginLobby(null);

        when(matchRepo.save(any(Match.class))).thenReturn(testMatch);

        // Act
        MatchDTO result = matchService.createMatch(testMatchDTO);

        // Assert
        assertNotNull(result);
        verify(matchRepo, times(1)).save(any(Match.class));
    }

    @Test
    void toEntity_WithNullOriginLobbyId_ShouldThrowException() {
        // Arrange
        LobbyDTO lobbyDTO = new LobbyDTO();
        lobbyDTO.setId(null);
        testMatchDTO.setOriginLobby(lobbyDTO);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            matchService.createMatch(testMatchDTO);
        });

        assertEquals("Origin lobby id is null – ensure the LobbyDTO is persistent", exception.getMessage());
        verify(matchRepo, never()).save(any(Match.class));
    }

    @Test
    void toDTO_WithNullTeams_ShouldHandleGracefully() {
        // Arrange
        testMatch.setTeamA(null);
        testMatch.setTeamB(null);

        when(matchRepo.findById("match123")).thenReturn(Optional.of(testMatch));

        // Act
        MatchDTO result = matchService.getMatch("match123");

        // Assert
        assertNotNull(result);
        assertNotNull(result.getTeamA());
        assertTrue(result.getTeamA().isEmpty());
        assertNotNull(result.getTeamB());
        assertTrue(result.getTeamB().isEmpty());

        verify(matchRepo, times(1)).findById("match123");
    }

    @Test
    void toDTO_WithNullOriginLobby_ShouldHandleGracefully() {
        // Arrange
        testMatch.setOriginLobby(null);

        when(matchRepo.findById("match123")).thenReturn(Optional.of(testMatch));

        // Act
        MatchDTO result = matchService.getMatch("match123");

        // Assert
        assertNotNull(result);
        assertNull(result.getOriginLobby());

        verify(matchRepo, times(1)).findById("match123");
    }
}
