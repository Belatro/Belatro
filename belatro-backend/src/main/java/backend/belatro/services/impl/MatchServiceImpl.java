package backend.belatro.services.impl;

import backend.belatro.dtos.MatchDTO;
import backend.belatro.dtos.UserUpdateDTO;
import backend.belatro.dtos.LobbyDTO;
import backend.belatro.models.Match;
import backend.belatro.models.User;
import backend.belatro.models.Lobbies;
import backend.belatro.repos.MatchRepo;
import backend.belatro.services.IMatchService;
import backend.belatro.util.MappingUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MatchServiceImpl implements IMatchService {

    private final MatchRepo matchRepo;

    @Autowired
    public MatchServiceImpl(MatchRepo matchRepo) {
        this.matchRepo = matchRepo;
    }

    @Override
    public MatchDTO createMatch(MatchDTO matchDTO) {
        Match match = toEntity(matchDTO);
        Match savedMatch = matchRepo.save(match);
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
                .map(existingMatch -> {
                    Match updatedMatch = toEntity(matchDTO);
                    updatedMatch.setId(existingMatch.getId());
                    return toDTO(matchRepo.save(updatedMatch));
                })
                .orElseThrow(() -> new RuntimeException("Match not found with id: " + id));
    }

    @Override
    public void deleteMatch(String id) {
        matchRepo.deleteById(id);
    }


    private Match toEntity(MatchDTO dto) {
        Match match = new Match();
        match.setId(dto.getId());
        match.setGameMode(dto.getGameMode());
        match.setMoves(dto.getMoves());
        match.setResult(dto.getResult());
        match.setStartTime(dto.getStartTime());
        match.setEndTime(dto.getEndTime());

        match.setTeamA(toUserEntities(dto.getTeamA()));
        match.setTeamB(toUserEntities(dto.getTeamB()));

        if (dto.getOriginLobby() != null) {
            match.setOriginLobby(toLobbyEntity(dto.getOriginLobby()));
        }
        return match;
    }

    private List<User> toUserEntities(List<UserUpdateDTO> userDtos) {
        if (userDtos == null) {
            return Collections.emptyList();
        }
        return userDtos.stream()
                .map(this::toUserEntity)
                .collect(Collectors.toList());
    }

    private User toUserEntity(UserUpdateDTO dto) {
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        return user;
    }

    private Lobbies toLobbyEntity(LobbyDTO dto) {
        Lobbies lobby = new Lobbies();
        BeanUtils.copyProperties(dto, lobby);

        if (dto.getHostUser() != null) {
            lobby.setHostUser(toUserEntitySimple(dto.getHostUser()));
        }

        dto.setTeamAPlayers(MappingUtils.mapList(lobby.getTeamAPlayers(), this::toUserSimpleDTO));
        dto.setTeamBPlayers(MappingUtils.mapList(lobby.getTeamBPlayers(), this::toUserSimpleDTO));
        dto.setUnassignedPlayers(MappingUtils.mapList(lobby.getUnassignedPlayers(), this::toUserSimpleDTO));

        return lobby;
    }


    private User toUserEntitySimple(LobbyDTO.UserSimpleDTO simpleDTO) {
        User user = new User();
        BeanUtils.copyProperties(simpleDTO, user);
        return user;
    }


    private MatchDTO toDTO(Match match) {
        MatchDTO dto = new MatchDTO();
        dto.setId(match.getId());
        dto.setGameMode(match.getGameMode());
        dto.setMoves(match.getMoves());
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

    private List<UserUpdateDTO> toUserDTOs(List<User> users) {
        if (users == null) {
            return Collections.emptyList();
        }
        return users.stream()
                .map(this::toUserDTO)
                .collect(Collectors.toList());
    }

    private UserUpdateDTO toUserDTO(User user) {
        UserUpdateDTO dto = new UserUpdateDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    private LobbyDTO toLobbyDTO(Lobbies lobby) {
        LobbyDTO dto = new LobbyDTO();
        BeanUtils.copyProperties(lobby, dto);

        if (lobby.getHostUser() != null) {
            dto.setHostUser(toUserSimpleDTO(lobby.getHostUser()));
        }

        dto.setTeamAPlayers(MappingUtils.mapList(lobby.getTeamAPlayers(), this::toUserSimpleDTO));
        dto.setTeamBPlayers(MappingUtils.mapList(lobby.getTeamBPlayers(), this::toUserSimpleDTO));
        dto.setUnassignedPlayers(MappingUtils.mapList(lobby.getUnassignedPlayers(), this::toUserSimpleDTO));

        return dto;
    }


    private LobbyDTO.UserSimpleDTO toUserSimpleDTO(User user) {
        LobbyDTO.UserSimpleDTO simpleDTO = new LobbyDTO.UserSimpleDTO();
        BeanUtils.copyProperties(user, simpleDTO);
        return simpleDTO;
    }
}
