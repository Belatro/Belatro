package backend.belatro.controllers;

import backend.belatro.dtos.JoinLobbyRequestDTO;
import backend.belatro.dtos.LobbyDTO;
import backend.belatro.dtos.MatchDTO;
import backend.belatro.dtos.TeamSwitchRequestDTO;
import backend.belatro.services.LobbyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class LobbyControllerTest {
    @Mock
    LobbyService lobbyService;

    @InjectMocks
    LobbyController lobbyController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateLobby() {
        // Arrange
        LobbyDTO lobbyDTO = new LobbyDTO();
        when(lobbyService.createLobby(any(LobbyDTO.class))).thenReturn(lobbyDTO);

        // Act
        ResponseEntity<LobbyDTO> result = lobbyController.createLobby(new LobbyDTO());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(lobbyService, times(1)).createLobby(any(LobbyDTO.class));
    }

    @Test
    void testJoinLobby() {
        // Arrange
        LobbyDTO lobbyDTO = new LobbyDTO();
        when(lobbyService.joinLobby(any(JoinLobbyRequestDTO.class))).thenReturn(lobbyDTO);

        // Act
        ResponseEntity<LobbyDTO> result = lobbyController.joinLobby(new JoinLobbyRequestDTO());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(lobbyService, times(1)).joinLobby(any(JoinLobbyRequestDTO.class));
    }

    @Test
    void testGetLobby() {
        // Arrange
        LobbyDTO lobbyDTO = new LobbyDTO();
        when(lobbyService.getLobby(anyString())).thenReturn(lobbyDTO);

        // Act
        ResponseEntity<LobbyDTO> result = lobbyController.getLobby("lobbyId");

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(lobbyService, times(1)).getLobby("lobbyId");
    }

    @Test
    void testUpdateLobby() {
        // Arrange
        LobbyDTO lobbyDTO = new LobbyDTO();
        when(lobbyService.updateLobby(any(LobbyDTO.class))).thenReturn(lobbyDTO);

        // Act
        ResponseEntity<LobbyDTO> result = lobbyController.updateLobby(new LobbyDTO());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(lobbyService, times(1)).updateLobby(any(LobbyDTO.class));
    }

    @Test
    void testDeleteLobby() {
        // Act
        ResponseEntity<Void> result = lobbyController.deleteLobby("lobbyId");

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(lobbyService, times(1)).deleteLobby("lobbyId");
    }

    @Test
    void testSwitchTeam() {
        // Arrange
        LobbyDTO lobbyDTO = new LobbyDTO();
        when(lobbyService.switchTeam(any(TeamSwitchRequestDTO.class))).thenReturn(lobbyDTO);

        // Act
        ResponseEntity<LobbyDTO> result = lobbyController.switchTeam(new TeamSwitchRequestDTO());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(lobbyService, times(1)).switchTeam(any(TeamSwitchRequestDTO.class));
    }

    @Test
    void testStartMatch() {
        // Arrange
        MatchDTO matchDTO = new MatchDTO();
        when(lobbyService.startMatch(anyString())).thenReturn(matchDTO);

        // Act
        ResponseEntity<MatchDTO> result = lobbyController.startMatch("lobbyId");

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        verify(lobbyService, times(1)).startMatch("lobbyId");
    }
}
