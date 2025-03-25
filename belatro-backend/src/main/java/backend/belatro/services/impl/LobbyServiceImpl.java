package backend.belatro.services.impl;

import backend.belatro.dtos.JoinLobbyRequestDTO;
import backend.belatro.dtos.LobbyDTO;
import backend.belatro.dtos.MatchDTO;
import backend.belatro.dtos.TeamSwitchRequestDTO;
import backend.belatro.enums.lobbyStatus;
import backend.belatro.dtos.UserUpdateDTO;
import backend.belatro.enums.GameMode;
import backend.belatro.models.Lobbies;
import backend.belatro.models.User;
import backend.belatro.repos.LobbiesRepo;
import backend.belatro.repos.UserRepo;
import backend.belatro.services.IMatchService;
import backend.belatro.services.LobbyService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LobbyServiceImpl implements LobbyService {
    private final LobbiesRepo lobbyRepo;
    private final UserRepo userRepo;
    private final IMatchService matchService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    public LobbyServiceImpl(LobbiesRepo lobbyRepo, UserRepo userRepo, IMatchService matchService) {
        this.lobbyRepo = lobbyRepo;
        this.userRepo = userRepo;
        this.matchService = matchService;
    }

    @Override
    public LobbyDTO createLobby(LobbyDTO lobbyDTO) {
        if (lobbyDTO.getHostUser() == null || lobbyDTO.getHostUser().getId() == null)
            throw new RuntimeException("Host user is required");
        if (lobbyDTO.isPrivateLobby() && (lobbyDTO.getPassword() == null || lobbyDTO.getPassword().trim().isEmpty()))
            throw new RuntimeException("Password required for private lobby");
        User host = userRepo.findById(lobbyDTO.getHostUser().getId())
                .orElseThrow(() -> new RuntimeException("Host user not found"));
        Lobbies lobby = new Lobbies();
        lobby.setName(lobbyDTO.getName());
        lobby.setGameMode("CASUAL");
        lobby.setStatus(lobbyDTO.getStatus());
        lobby.setHostUser(host);
        lobby.setTeamAPlayers(new ArrayList<>());
        lobby.getTeamAPlayers().add(host);
        lobby.setTeamBPlayers(new ArrayList<>());
        lobby.setUnassignedPlayers(new ArrayList<>());
        lobby.setCreatedAt(new Date());
        lobby.setPrivateLobby(lobbyDTO.isPrivateLobby());
        if (lobbyDTO.isPrivateLobby()) {
            String hashedPassword = encoder.encode(lobbyDTO.getPassword());
            lobby.setPassword(hashedPassword);
        } else {
            lobby.setPassword(null);
        }
        lobby = lobbyRepo.save(lobby);
        return mapToDTO(lobby);
    }

    @Override
    public LobbyDTO joinLobby(JoinLobbyRequestDTO joinRequest) {
        Lobbies lobby = lobbyRepo.findById(joinRequest.getLobbyId())
                .orElseThrow(() -> new RuntimeException("Lobby not found"));
        if(lobby.isPrivateLobby()){
            if(joinRequest.getPassword() == null || joinRequest.getPassword().trim().isEmpty())
                throw new RuntimeException("Password required for private lobby");
            if(!encoder.matches(joinRequest.getPassword(), lobby.getPassword()))
                throw new RuntimeException("Invalid lobby password");
        }
        int totalPlayers = lobby.getTeamAPlayers().size() + lobby.getTeamBPlayers().size() + lobby.getUnassignedPlayers().size();
        if(totalPlayers >= 4)
            throw new RuntimeException("Lobby is full");
        User user = userRepo.findById(joinRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean alreadyPresent = lobby.getTeamAPlayers().stream().anyMatch(u -> u.getId().equals(user.getId()))
                || lobby.getTeamBPlayers().stream().anyMatch(u -> u.getId().equals(user.getId()))
                || lobby.getUnassignedPlayers().stream().anyMatch(u -> u.getId().equals(user.getId()));
        if (!alreadyPresent) {
            lobby.getUnassignedPlayers().add(user);
            lobby = lobbyRepo.save(lobby);
        }
        return mapToDTO(lobby);
    }

    @Override
    public LobbyDTO getLobby(String lobbyId) {
        Lobbies lobby = lobbyRepo.findById(lobbyId).orElseThrow(() -> new RuntimeException("Lobby not found"));
        return mapToDTO(lobby);
    }

    @Override
    public LobbyDTO updateLobby(LobbyDTO lobbyDTO) {
        Lobbies lobby = lobbyRepo.findById(lobbyDTO.getId()).orElseThrow(() -> new RuntimeException("Lobby not found"));
        lobby.setName(lobbyDTO.getName());
        lobby.setGameMode("CASUAL");
        lobby.setStatus(lobbyDTO.getStatus());
        lobby.setPrivateLobby(lobbyDTO.isPrivateLobby());
        if(lobbyDTO.isPrivateLobby()){
            if(lobbyDTO.getPassword() == null || lobbyDTO.getPassword().trim().isEmpty())
                throw new RuntimeException("Password required for private lobby");
            lobby.setPassword(encoder.encode(lobbyDTO.getPassword()));
        } else {
            lobby.setPassword(null);
        }
        lobby = lobbyRepo.save(lobby);
        return mapToDTO(lobby);
    }

    @Override
    public void deleteLobby(String lobbyId) {
        lobbyRepo.deleteById(lobbyId);
    }

    @Override
    public LobbyDTO switchTeam(TeamSwitchRequestDTO switchRequest) {
        Lobbies lobby = lobbyRepo.findById(switchRequest.getLobbyId())
                .orElseThrow(() -> new RuntimeException("Lobby not found"));
        User user = userRepo.findById(switchRequest.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean inA = lobby.getTeamAPlayers().stream().anyMatch(u -> u.getId().equals(user.getId()));
        boolean inB = lobby.getTeamBPlayers().stream().anyMatch(u -> u.getId().equals(user.getId()));
        boolean inU = lobby.getUnassignedPlayers().stream().anyMatch(u -> u.getId().equals(user.getId()));
        if(!(inA || inB || inU))
            throw new RuntimeException("User is not in the lobby");
        lobby.getTeamAPlayers().removeIf(u -> u.getId().equals(user.getId()));
        lobby.getTeamBPlayers().removeIf(u -> u.getId().equals(user.getId()));
        lobby.getUnassignedPlayers().removeIf(u -> u.getId().equals(user.getId()));
        String targetTeam = switchRequest.getTargetTeam();
        if(targetTeam == null || targetTeam.trim().isEmpty())
            throw new RuntimeException("Target team is required");
        targetTeam = targetTeam.toUpperCase();
        switch(targetTeam) {
            case "A":
                if(lobby.getTeamAPlayers().size() >= 2)
                    throw new RuntimeException("Team A is full");
                lobby.getTeamAPlayers().add(user);
                break;
            case "B":
                if(lobby.getTeamBPlayers().size() >= 2)
                    throw new RuntimeException("Team B is full");
                lobby.getTeamBPlayers().add(user);
                break;
            case "U":
                if(lobby.getUnassignedPlayers().size() >= 4)
                    throw new RuntimeException("Unassigned is full");
                lobby.getUnassignedPlayers().add(user);
                break;
            default:
                throw new RuntimeException("Invalid target team. Use A, B, or U.");
        }
        lobby = lobbyRepo.save(lobby);
        return mapToDTO(lobby);
    }
    @Override
    public MatchDTO startMatch(String lobbyId) {
        Lobbies lobby = lobbyRepo.findById(lobbyId)
                .orElseThrow(() -> new RuntimeException("Lobby not found"));
        int totalPlayers = lobby.getTeamAPlayers().size() + lobby.getTeamBPlayers().size();
        if (totalPlayers < 4) {
            throw new RuntimeException("Cannot start match: there must be at least 4 players across both teams");
        }
        lobby.setStatus(lobbyStatus.CLOSED);
        lobby = lobbyRepo.save(lobby);
        List<LobbyDTO.UserSimpleDTO> teamA = lobby.getTeamAPlayers().stream()
                .map(this::convertUserToUserSimple)
                .collect(Collectors.toList());
        List<LobbyDTO.UserSimpleDTO> teamB = lobby.getTeamBPlayers().stream()
                .map(this::convertUserToUserSimple)
                .collect(Collectors.toList());
        MatchDTO matchDTO = new MatchDTO();
        matchDTO.setTeamA(teamA);
        matchDTO.setTeamB(teamB);
        LobbyDTO originLobby = mapToDTO(lobby);
        matchDTO.setOriginLobby(originLobby);
        if ("RANKED".equalsIgnoreCase(lobby.getGameMode())) {
            matchDTO.setGameMode(GameMode.RANKED);
        } else {
            matchDTO.setGameMode(GameMode.CASUAL);
        }
        matchDTO.setStartTime(new Date());
        matchDTO.setMoves(null);
        matchDTO.setResult(null);
        return matchService.createMatch(matchDTO);
    }

    private LobbyDTO.UserSimpleDTO convertUserToUserSimple(User user) {
        LobbyDTO.UserSimpleDTO dto = new LobbyDTO.UserSimpleDTO();
        BeanUtils.copyProperties(user, dto);
        return dto;
    }

    private LobbyDTO mapToDTO(Lobbies lobby) {
        LobbyDTO dto = new LobbyDTO();
        dto.setId(lobby.getId());
        dto.setName(lobby.getName());
        dto.setGameMode(lobby.getGameMode());
        dto.setStatus(lobby.getStatus());
        dto.setCreatedAt(lobby.getCreatedAt());
        if (lobby.getHostUser() != null) {
            LobbyDTO.UserSimpleDTO hostDto = new LobbyDTO.UserSimpleDTO();
            hostDto.setId(lobby.getHostUser().getId());
            hostDto.setUsername(lobby.getHostUser().getUsername());
            dto.setHostUser(hostDto);
        }
        dto.setTeamAPlayers(lobby.getTeamAPlayers().stream().map(u -> {
            LobbyDTO.UserSimpleDTO udto = new LobbyDTO.UserSimpleDTO();
            udto.setId(u.getId());
            udto.setUsername(u.getUsername());
            return udto;
        }).collect(Collectors.toList()));
        dto.setTeamBPlayers(lobby.getTeamBPlayers().stream().map(u -> {
            LobbyDTO.UserSimpleDTO udto = new LobbyDTO.UserSimpleDTO();
            udto.setId(u.getId());
            udto.setUsername(u.getUsername());
            return udto;
        }).collect(Collectors.toList()));
        dto.setUnassignedPlayers(lobby.getUnassignedPlayers().stream().map(u -> {
            LobbyDTO.UserSimpleDTO udto = new LobbyDTO.UserSimpleDTO();
            udto.setId(u.getId());
            udto.setUsername(u.getUsername());
            return udto;
        }).collect(Collectors.toList()));
        dto.setPrivateLobby(lobby.isPrivateLobby());
        dto.setPassword(null);
        return dto;
    }
}

