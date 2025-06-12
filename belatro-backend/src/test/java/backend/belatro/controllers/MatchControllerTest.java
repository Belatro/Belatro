package backend.belatro.controllers;

import backend.belatro.dtos.MatchDTO;
import backend.belatro.services.IMatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class MatchControllerTest {
    @Mock
    private IMatchService matchService;

    @InjectMocks
    private MatchController matchController;

    private MatchDTO testMatchDTO;
    private List<MatchDTO> testMatchDTOs;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test data
        testMatchDTO = new MatchDTO();
        testMatchDTO.setId("match123");

        MatchDTO matchDTO2 = new MatchDTO();
        matchDTO2.setId("match456");

        testMatchDTOs = Arrays.asList(testMatchDTO, matchDTO2);
    }

    @Test
    void createMatch_ShouldReturnCreatedMatch() {
        // Arrange
        when(matchService.createMatch(any(MatchDTO.class))).thenReturn(testMatchDTO);

        // Act
        ResponseEntity<MatchDTO> result = matchController.createMatch(new MatchDTO());

        // Assert
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("match123", result.getBody().getId());
        verify(matchService, times(1)).createMatch(any(MatchDTO.class));
    }

    @Test
    void getMatch_ShouldReturnMatch() {
        // Arrange
        when(matchService.getMatch("match123")).thenReturn(testMatchDTO);

        // Act
        ResponseEntity<MatchDTO> result = matchController.getMatch("match123");

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("match123", result.getBody().getId());
        verify(matchService, times(1)).getMatch("match123");
    }

    @Test
    void getMatch_WhenMatchNotFound_ShouldReturnNotFound() {
        // Arrange
        when(matchService.getMatch("nonexistent")).thenReturn(null);

        // Act
        ResponseEntity<MatchDTO> result = matchController.getMatch("nonexistent");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertNull(result.getBody());
        verify(matchService, times(1)).getMatch("nonexistent");
    }

    @Test
    void getAllMatches_ShouldReturnAllMatches() {
        // Arrange
        when(matchService.getAllMatches()).thenReturn(testMatchDTOs);

        // Act
        ResponseEntity<List<MatchDTO>> result = matchController.getAllMatches();

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().size());
        verify(matchService, times(1)).getAllMatches();
    }

    @Test
    void updateMatch_ShouldReturnUpdatedMatch() {
        // Arrange
        when(matchService.updateMatch(anyString(), any(MatchDTO.class))).thenReturn(testMatchDTO);

        // Act
        ResponseEntity<MatchDTO> result = matchController.updateMatch("match123", new MatchDTO());

        // Assert
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("match123", result.getBody().getId());
        verify(matchService, times(1)).updateMatch(anyString(), any(MatchDTO.class));
    }

    @Test
    void updateMatch_WhenMatchNotFound_ShouldReturnNotFound() {
        // Arrange
        when(matchService.updateMatch(anyString(), any(MatchDTO.class)))
                .thenThrow(new RuntimeException("Match not found"));

        // Act
        ResponseEntity<MatchDTO> result = matchController.updateMatch("nonexistent", new MatchDTO());

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertNull(result.getBody());
        verify(matchService, times(1)).updateMatch(anyString(), any(MatchDTO.class));
    }

    @Test
    void deleteMatch_ShouldReturnNoContent() {
        // Act
        ResponseEntity<Void> result = matchController.deleteMatch("match123");

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(matchService, times(1)).deleteMatch("match123");
    }
}
