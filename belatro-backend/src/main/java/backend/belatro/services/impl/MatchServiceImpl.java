package backend.belatro.services.impl;

import backend.belatro.dtos.*;
import backend.belatro.enums.MoveType;
import backend.belatro.models.Lobbies;
import backend.belatro.models.Match;
import backend.belatro.models.MatchMove;
import backend.belatro.models.User;
import backend.belatro.repos.MatchMoveRepo;
import backend.belatro.repos.MatchRepo;
import backend.belatro.services.IMatchService;
import backend.belatro.util.MappingUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchServiceImpl implements IMatchService {

    private final MatchRepo matchRepo;
    private final MatchMoveRepo matchMoveRepo;

    @Autowired
    public MatchServiceImpl(MatchRepo matchRepo, MatchMoveRepo matchMoveRepo) {
        this.matchRepo = matchRepo;
        this.matchMoveRepo = matchMoveRepo;
    }

    @Override
    public MatchDTO createMatch(MatchDTO matchDTO) {
        System.out.println("Entering createMatch with MatchDTO: " + matchDTO);
        Match match = toEntity(matchDTO);
        Match savedMatch = matchRepo.save(match);
        System.out.println("Saved Match id: " + savedMatch.getId());
        return toDTO(savedMatch);
    }

    @Override
    public MatchDTO getMatch(String id) {
        return matchRepo.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    public List<MatchDTO> getAllMatches() {
        return matchRepo.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public MatchDTO updateMatch(String id, MatchDTO matchDTO) {
        return matchRepo.findById(id)
                .map(existing -> {
                    Match updated = toEntity(matchDTO);
                    updated.setId(existing.getId());
                    return toDTO(matchRepo.save(updated));
                })
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + id));
    }

    @Override
    public void deleteMatch(String id) {
        matchRepo.deleteById(id);
    }

    @Transactional
    public void recordMove(String matchId,
                           MoveType type,                     // e.g. "PLAY_CARD", "BID"
                           Map<String, Object> payload,     // arbitrary JSON-ish info
                           double evaluation) {

        int nextNo = (int) matchMoveRepo.countByMatchId(matchId) + 1;

        // 2) Persist
        MatchMove move = MatchMove.builder()
                .matchId   (matchId)
                .number    (nextNo)           // globally ordered within the match
                .type      (type)
                .payload   (payload)
                .evaluation(evaluation)
                .ts        (Instant.now())
                .build();

        matchMoveRepo.save(move);
    }

    @Override
    public List<HandDTO> getStructuredMoves(String matchId) {

        List<MatchMove> movesFlat = matchMoveRepo.findByMatchIdOrderByNumber(matchId);

        // handNo ‑> trickNo ‑> moves
        Map<Integer, Map<Integer, List<MatchMove>>> grouped =
                movesFlat.stream()
                        .collect(Collectors.groupingBy(
                                mv -> (Integer) mv.getPayload().getOrDefault("handNo", 0),
                                LinkedHashMap::new,
                                Collectors.groupingBy(
                                        mv -> (Integer) mv.getPayload().getOrDefault("trickNo", 0),
                                        LinkedHashMap::new,
                                        Collectors.toList()
                                )));

        return grouped.entrySet().stream()
                .map(hand -> {
                    int handNo = hand.getKey();
                    List<TrickDTO> tricks = hand.getValue().entrySet().stream()
                            .map(trick -> {
                                int trickNo = trick.getKey();
                                List<MoveDTO> moves = trick.getValue().stream()
                                        .sorted(Comparator.comparingInt(MatchMove::getNumber))
                                        .map(mm -> new MoveDTO(
                                                mm.getNumber(),
                                                (String) mm.getPayload().get("playerId"),
                                                (String) mm.getPayload().get("card")))
                                        .toList();

                                String trump = trick.getValue().stream()
                                        .map(mm -> (String) mm.getPayload().get("trump"))
                                        .filter(Objects::nonNull)
                                        .findFirst()
                                        .orElse(null);

                                return new TrickDTO(trickNo, trump, moves);
                            })
                            .toList();

                    return new HandDTO(handNo, tricks);
                })
                .toList();
    }

    @Override
    public List<MoveDTO> getMoves(String matchId) {
        return matchMoveRepo.findByMatchIdOrderByNumber(matchId)
                .stream()
                .map(this::toMoveDTO)
                .toList();
    }

    private MoveDTO toMoveDTO(MatchMove mm) {
        return new MoveDTO(
                mm.getNumber(),
                (String) mm.getPayload().get("playerId"),
                (String) mm.getPayload().get("card")
        );
    }





    private Match toEntity(MatchDTO dto) {
        System.out.println("Converting MatchDTO to Match. DTO id: " + dto.getId());
        Match match = new Match();
        match.setId(dto.getId());
        match.setGameMode(dto.getGameMode());
        match.setResult(dto.getResult());
        match.setStartTime(dto.getStartTime());
        match.setEndTime(dto.getEndTime());
        match.setTeamA(toUserEntities(dto.getTeamA()));
        match.setTeamB(toUserEntities(dto.getTeamB()));
        if (dto.getOriginLobby() != null) {
            String lobbyId = dto.getOriginLobby().getId();
            System.out.println("Origin lobby id from DTO: " + lobbyId);
            if (lobbyId == null) {
                throw new RuntimeException("Origin lobby id is null – ensure the LobbyDTO is persistent");
            }
            Lobbies lobbyRef = new Lobbies();
            lobbyRef.setId(lobbyId);
            System.out.println("Using lobby stub with id: " + lobbyRef.getId());
            match.setOriginLobby(lobbyRef);
        }
        return match;
    }


    private List<User> toUserEntities(List<LobbyDTO.UserSimpleDTO> dtos) {
        if (dtos == null) {
            System.out.println("UserSimpleDTO list is null or empty.");
            return Collections.emptyList();
        }
        return dtos.stream()
                .map(this::toUserEntity)
                .collect(Collectors.toList());
    }

    private User toUserEntity(LobbyDTO.UserSimpleDTO dto) {
        System.out.println("Converting UserSimpleDTO to User. DTO: " + dto);
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        System.out.println("Converted User id: " + user.getId());
        return user;
    }

    private MatchDTO toDTO(Match match) {
        System.out.println("Converting Match to MatchDTO. Match id: " + match.getId());
        MatchDTO dto = new MatchDTO();
        dto.setId(match.getId());
        dto.setGameMode(match.getGameMode());
        dto.setResult(match.getResult());
        dto.setStartTime(match.getStartTime());
        dto.setEndTime(match.getEndTime());
        dto.setTeamA(toUserDTOs(match.getTeamA()));
        dto.setTeamB(toUserDTOs(match.getTeamB()));
        if (match.getOriginLobby() != null) {
            dto.setOriginLobby(toLobbyDTO(match.getOriginLobby()));
        }
        return dto;
    }

    private List<LobbyDTO.UserSimpleDTO> toUserDTOs(List<User> users) {
        if (users == null) {
            System.out.println("User list is null or empty.");
            return Collections.emptyList();
        }
        return users.stream()
                .map(this::toUserSimpleDTO)
                .collect(Collectors.toList());
    }

    private LobbyDTO.UserSimpleDTO toUserSimpleDTO(User user) {
        System.out.println("Converting User to UserSimpleDTO. User id: " + user.getId());
        LobbyDTO.UserSimpleDTO simple = new LobbyDTO.UserSimpleDTO();
        simple.setId(user.getId());
        simple.setUsername(user.getUsername());
        System.out.println("Converted UserSimpleDTO: " + simple);
        return simple;
    }

    private LobbyDTO toLobbyDTO(Lobbies lobby) {
        System.out.println("Converting Lobbies to LobbyDTO. Lobby id: " + lobby.getId());
        LobbyDTO dto = new LobbyDTO();
        BeanUtils.copyProperties(lobby, dto);
        if (lobby.getHostUser() != null) {
            dto.setHostUser(toUserSimpleDTO(lobby.getHostUser()));
        }
        dto.setTeamAPlayers(MappingUtils.mapList(lobby.getTeamAPlayers(), this::toUserSimpleDTO));
        dto.setTeamBPlayers(MappingUtils.mapList(lobby.getTeamBPlayers(), this::toUserSimpleDTO));
        dto.setUnassignedPlayers(MappingUtils.mapList(lobby.getUnassignedPlayers(), this::toUserSimpleDTO));
        System.out.println("Converted LobbyDTO: " + dto);
        return dto;
    }
}
