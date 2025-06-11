package backend.belatro.services;

import backend.belatro.dtos.MatchDTO;
import backend.belatro.dtos.RematchStartedEvent;
import backend.belatro.enums.GameMode;
import backend.belatro.pojo.gamelogic.BelotGame;
import backend.belatro.pojo.gamelogic.Player;
import backend.belatro.pojo.gamelogic.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RematchServiceTest {
    @Mock RedisTemplate<String,String> redis;
    @Mock SetOperations<String,String> setOps;
    @Mock BelotGameService gameSvc;
    @Mock IMatchService    matchSvc;
    @Mock ApplicationEventPublisher bus;

    @InjectMocks RematchService rematchService;

    private List<Player> players;
    private Team teamA, teamB;

    @BeforeEach
    void setUp() {
        when(redis.opsForSet()).thenReturn(setOps);

        // 1) create your oldGame and its players
        BelotGame oldGame = mock(BelotGame.class);
        players = List.of("A","B","C","D").stream()
                .map(Player::new)
                .toList();
        when(oldGame.getPlayers()).thenReturn(players);
        when(gameSvc.get("old")).thenReturn(oldGame);

        // 2) **stub teamA & teamB** so they’re not null
        teamA = new Team(List.of(players.get(0), players.get(1)));
        teamB = new Team(List.of(players.get(2), players.get(3)));
        when(oldGame.getTeamA()).thenReturn(teamA);
        when(oldGame.getTeamB()).thenReturn(teamB);

        // 3) stub matchSvc.getMatch(...) & createMatch(...) as before…
        MatchDTO oldDto = new MatchDTO();
        oldDto.setGameMode(GameMode.CASUAL);
        // build your DTO teams the same way, set originLobby, startTime, etc.
        when(matchSvc.getMatch("old")).thenReturn(oldDto);
        when(matchSvc.createMatch(any(MatchDTO.class))).thenAnswer(inv -> {
            MatchDTO dto = inv.getArgument(0, MatchDTO.class);
            dto.setId("NEW123");
            return dto;
        });

        // 4) back your Redis voting set with long-returning doAnswer
        Set<String> votes = new HashSet<>();
        doAnswer(inv -> votes.add(inv.getArgument(1)) ? 1L : 0L)
                .when(setOps).add(anyString(), anyString());
        when(setOps.members(anyString()))
                .thenAnswer(inv -> Collections.unmodifiableSet(votes));
    }

    @Test
    void startsNewGameWhenEveryoneAccepts() {
        // Act: each of the 4 players votes “yes”
        players.forEach(p -> rematchService.requestOrAccept("old", p.getId()));

        // Now gameSvc.start will have been called with your real teamA/teamB
        verify(matchSvc, times(1)).createMatch(any(MatchDTO.class));
        verify(gameSvc,  times(1))
                .start("NEW123", teamA, teamB);

        // And an event was published
        ArgumentCaptor<RematchStartedEvent> cap = ArgumentCaptor.forClass(RematchStartedEvent.class);
        verify(bus).publishEvent(cap.capture());
        assertThat(cap.getValue().oldGameId()).isEqualTo("old");
        assertThat(cap.getValue().newGameId()).isEqualTo("NEW123");

        // Finally, the Redis key gets a TTL
        verify(redis, atLeastOnce())
                .expire("belot:rematch:old:accepted", Duration.ofMinutes(2));
    }
}
