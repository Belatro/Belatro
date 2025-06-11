package backend.belatro.services.impl;

import backend.belatro.dtos.*;
import backend.belatro.enums.GameMode;
import backend.belatro.enums.MoveType;
import backend.belatro.exceptions.NotFoundException;
import backend.belatro.models.Lobbies;
import backend.belatro.models.Match;
import backend.belatro.models.MatchMove;
import backend.belatro.models.User;
import backend.belatro.repos.MatchMoveRepo;
import backend.belatro.repos.MatchRepo;
import backend.belatro.services.IMatchService;
import backend.belatro.services.RankHistoryService;
import backend.belatro.util.MappingUtils;
import backend.belatro.util.MatchUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


@Service
public class MatchServiceImpl implements IMatchService {

    private final MatchRepo matchRepo;
    private final MatchMoveRepo matchMoveRepo;
    private final RankHistoryService rankHistoryService;
    private static final int PLAYS_PER_HAND  = 32;
    private static final int PLAYS_PER_TRICK = 4;
    private static final AtomicLong GLOBAL_MOVE_SEQUENCE = new AtomicLong();


    @Autowired
    public MatchServiceImpl(MatchRepo matchRepo, MatchMoveRepo matchMoveRepo, RankHistoryService rankHistoryService) {
        this.matchRepo = matchRepo;
        this.matchMoveRepo = matchMoveRepo;
        this.rankHistoryService = rankHistoryService;
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
    @Override
    public void recordMove(String matchId,
                           MoveType moveType,
                           Map<String, Object> payload,
                           double evaluation) {

        long totalCardPlays = calculateTotalCardPlays(matchId, moveType);

        // Only calculate hand/trick numbers for PLAY_CARD moves
        int handNo = 1;
        int trickNo = 1;

        if (moveType == MoveType.PLAY_CARD) {
            handNo = calculateHandNumber(totalCardPlays);
            trickNo = calculateTrickNumber(totalCardPlays);
        } else {
            Optional<MatchMove> lastCardMove = matchMoveRepo.findFirstByMatchIdAndTypeOrderByNumberDesc(matchId, MoveType.PLAY_CARD);
            if (lastCardMove.isPresent()) {
                handNo = lastCardMove.get().getHandNo();
            }
        }

        MatchMove move = new MatchMove();
        move.setMatchId(matchId);
        move.setNumber((int) GLOBAL_MOVE_SEQUENCE.incrementAndGet());
        move.setType(moveType);
        move.setPayload(payload);
        move.setEvaluation(evaluation);
        move.setHandNo(handNo);
        move.setTrickNo(trickNo);
        move.setTs(Instant.now());

        matchMoveRepo.save(move);
    }

    private long calculateTotalCardPlays(String matchId, MoveType moveType) {
        long existingCardPlays = matchMoveRepo.countByMatchIdAndType(matchId, MoveType.PLAY_CARD);

        if (moveType == MoveType.PLAY_CARD) {
            existingCardPlays++;
        }

        return existingCardPlays;
    }

    private int calculateHandNumber(long totalCardPlays) {
        return (int) ((totalCardPlays - 1) / PLAYS_PER_HAND) + 1;
    }

    private int calculateTrickNumber(long totalCardPlays) {
        long cardInCurrentHand = ((totalCardPlays - 1) % PLAYS_PER_HAND) + 1;
        return (int) ((cardInCurrentHand - 1) / PLAYS_PER_TRICK) + 1;
    }



    @Override
    public List<HandDTO> getStructuredMoves(String matchId) {

        List<MatchMove> moves = matchMoveRepo.findByMatchIdOrderByNumber(matchId);

        /* Helper record for tracking tricks in progress */
        record InProgress(List<MoveDTO> moves, int trickNo) {}

        /* 1. Prepare containers */
        Map<Integer, List<TrumpCallDTO>> trumpByHand   = new LinkedHashMap<>();
        Map<Integer, List<TrickDTO>>     tricksByHand  = new LinkedHashMap<>();
        Map<Integer, List<ChallengeDTO>>  challByHand   = new LinkedHashMap<>();   // <── NEW
        Map<Integer, InProgress>         currentTricks = new HashMap<>();

        /* state that replaces the incorrect move.getHandNo() */
        int handIdx       = 1;
        int cardsInHand   = 0;

        /* helper: flush a trick for a given hand */
        BiConsumer<Integer, InProgress> flush =
                (h, ip) -> {
                    if (!ip.moves().isEmpty()) {
                        tricksByHand.get(h)
                                .add(new TrickDTO(ip.trickNo(), List.copyOf(ip.moves())));
                        currentTricks.put(h, new InProgress(new ArrayList<>(), ip.trickNo() + 1));
                    }
                };

        for (MatchMove mv : moves) {

            if (mv.getType() == MoveType.BID && cardsInHand == PLAYS_PER_HAND) {
                // Close out any in-progress trick in the old hand:
                flush.accept(handIdx, currentTricks.get(handIdx));
                // Move to the next hand:
                handIdx++;
                // And reset the count of cards seen in the new hand
                cardsInHand = 0;
                // (We do NOT yet consume any “play” card, because this is still a BID.)
            }

            if (mv.getType() == MoveType.PLAY_CARD) {
                cardsInHand++;
                if (cardsInHand > PLAYS_PER_HAND) {

                    flush.accept(handIdx, currentTricks.get(handIdx));
                    handIdx++;
                    cardsInHand = 1; // the current card is the first card of the new hand
                }
            }
            int h = handIdx;                       // effective hand for this move

            currentTricks.computeIfAbsent(h, k -> new InProgress(new ArrayList<>(), 1));
            tricksByHand .computeIfAbsent(h, k -> new ArrayList<>());
            challByHand  .computeIfAbsent(h, k -> new ArrayList<>());           // <── NEW
            trumpByHand  .computeIfAbsent(h, k -> new ArrayList<>());

            InProgress ip = currentTricks.get(h);

            switch (mv.getType()) {

                case BID -> {

                    Boolean passFlag = (Boolean) mv.getPayload().get("pass");
                    String trumpVal;

                    if (Boolean.TRUE.equals(passFlag)) {
                        trumpVal = "PASS";
                    } else {
                        trumpVal = (String) mv.getPayload().get("trump");
                    }

                    trumpByHand.get(h).add(new TrumpCallDTO(
                            mv.getNumber(),
                            (String) mv.getPayload().get("playerId"),
                            trumpVal
                    ));
                }

                case PLAY_CARD -> {
                    ip.moves().add(new MoveDTO(
                            mv.getNumber(),
                            (String) mv.getPayload().get("playerId"),
                            (String) mv.getPayload().get("card")));

                    if (ip.moves().size() == 4) {
                        flush.accept(h, ip);
                    }
                }
                case CHALLENGE -> {
                    boolean success = Boolean.TRUE.equals(mv.getPayload().get("success"));
                    String  pid     = (String) mv.getPayload().get("playerId");

                    challByHand.get(handIdx)
                            .add(new ChallengeDTO(mv.getNumber(), pid, success));

                    if (success) {

                        flush.accept(handIdx, ip);


                        handIdx++;
                        cardsInHand = 0;

                    }
                }


                case END_TRICK -> flush.accept(h, ip);

                default -> { /* ignore */ }
            }
        }

        /* close leftovers at EOF */
        currentTricks.forEach(flush);

        return tricksByHand.entrySet().stream()
                .map(e -> new HandDTO(
                        e.getKey(),
                        trumpByHand .getOrDefault(e.getKey(), List.of()),
                        tricksByHand.get(e.getKey()),
                        challByHand .getOrDefault(e.getKey(), List.of())   // <── NEW
                ))
                .toList();
    }


    @Transactional
    @Override
    public void finaliseMatch(String matchId,
                              String winnerString,
                              Instant endTs) {

        matchRepo.findById(matchId).ifPresent(match -> {
            match.setResult(winnerString);
            match.setEndTime(Date.from(endTs));
            matchRepo.save(match);                           // already there

            if (match.getGameMode() == GameMode.RANKED) {
                rankHistoryService.updateRatingsForMatch(match);
            }
        });
    }


    @Override
    public List<MoveDTO> getMoves(String matchId) {
        return matchMoveRepo.findByMatchIdOrderByNumber(matchId)
                .stream()
                .map(this::toMoveDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MatchDTO getMatchByLobbyId(String lobbyId) {
        return matchRepo.findByOriginLobbyId(lobbyId)
                .map(this::toDTO)
                .orElseThrow(() -> new NotFoundException("Match for lobby " + lobbyId + " not found"));
    }

    @Transactional(readOnly = true)
    @Override
    public MatchHistoryDTO getMatchHistory(String matchId) {
        var match = getMatch(matchId);
        if (match == null) {
            throw new NotFoundException("Match not found: " + matchId);
        }
        List<MoveDTO> moves      = getMoves(matchId);
        List<HandDTO> structured = getStructuredMoves(matchId);
        return new MatchHistoryDTO(match, moves, structured);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<MatchHistoryDTO> getMatchHistoryByPlayer(String playerId,
                                                         Pageable pageable) {
        Page<Match> matches = matchRepo
                .findByPlayerIdInTeams(playerId, pageable);

        return matches.map(m -> {
            // use your existing toDTO instead of MatchMapper
            MatchDTO matchDto = toDTO(m);

            List<MoveDTO> rawMoves = getMoves(m.getId());
            List<HandDTO>  hands    = getStructuredMoves(m.getId());

            return new MatchHistoryDTO(matchDto, rawMoves, hands);
        });
    }
    @Override
    @Transactional(readOnly = true)
    public Page<PlayerMatchHistoryDTO> getFinishedMatchHistoryByPlayer(
            String playerId,
            Pageable pageable
    ) {
        int skip      = (int) pageable.getOffset();      // page * size
        int toCollect = pageable.getPageSize();

        List<PlayerMatchHistoryDTO> allForPlayer = new ArrayList<>();

        // We’ll page through finished matches until we’ve collected enough
        int repoPage = 0;
        while (allForPlayer.size() < skip + toCollect) {
            Page<Match> page = matchRepo.findByResultIsNotNull(
                    PageRequest.of(repoPage++, toCollect, Sort.by("endTime").descending())
            );
            if (page.isEmpty()) break;

            for (Match m : page.getContent()) {
                // in‐memory check of DBRef ids as strings
                boolean onA = m.getTeamA()
                        .stream()
                        .anyMatch(u -> u.getId().equals(playerId));
                boolean onB = m.getTeamB()
                        .stream()
                        .anyMatch(u -> u.getId().equals(playerId));
                if (!(onA||onB)) continue;

                // build your DTO
                MatchDTO dto        = toDTO(m);
                List<MoveDTO> raw   = getMoves(m.getId());
                List<HandDTO> hands = getStructuredMoves(m.getId());
                MatchHistoryDTO hist= new MatchHistoryDTO(dto, raw, hands);
                String outcome      = MatchUtils.isPlayerOnWinningTeam(dto,playerId)
                        ? "WIN" : "LOSS";

                allForPlayer.add(new PlayerMatchHistoryDTO(hist, outcome));
            }
            if (!page.hasNext()) break;
        }

        // Now materialize just the slice the user asked for:
        List<PlayerMatchHistoryDTO> pageContent = allForPlayer.stream()
                .skip(skip)
                .limit(toCollect)
                .toList();

        return new PageImpl<>(
                pageContent,
                pageable,
                allForPlayer.size()
        );
    }
    @Transactional(readOnly = true)
    @Override
    public Page<PlayerMatchSummaryDTO> getMatchSummariesByPlayer(
            String playerId,
            Pageable pageable
    ) {
        int skip      = (int) pageable.getOffset();
        int limit     = pageable.getPageSize();
        List<PlayerMatchSummaryDTO> allSummaries = new ArrayList<>();

        int repoPage = 0;
        // keep loading pages of finished matches until we have enough
        while (allSummaries.size() < skip + limit) {
            Page<Match> page = matchRepo.findByResultIsNotNull(
                    PageRequest.of(repoPage++, limit, Sort.by("endTime").descending())
            );
            if (page.isEmpty()) break;

            for (Match m : page.getContent()) {
                boolean onA = m.getTeamA()
                        .stream()
                        .anyMatch(u -> u.getId().equals(playerId));
                boolean onB = m.getTeamB()
                        .stream()
                        .anyMatch(u -> u.getId().equals(playerId));
                if (!onA && !onB) continue;

                boolean won = MatchUtils.isPlayerOnWinningTeam(toDTO(m), playerId);
                allSummaries.add(new PlayerMatchSummaryDTO(
                        m.getId(),
                        m.getEndTime().toInstant(),
                        m.getResult(),
                        won ? "WIN" : "LOSS",
                        m.getGameMode()
                ));
            }
            if (!page.hasNext()) break;
        }

        List<PlayerMatchSummaryDTO> pageContent = allSummaries.stream()
                .skip(skip)
                .limit(limit)
                .toList();

        return new PageImpl<>(
                pageContent,
                pageable,
                allSummaries.size()
        );
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
//        System.out.println("Converting UserSimpleDTO to User. DTO: " + dto);
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        System.out.println("Converted User id: " + user.getId());
        return user;
    }

    private MatchDTO toDTO(Match match) {
//        System.out.println("Converting Match to MatchDTO. Match id: " + match.getId());
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
//        System.out.println("Converting User to UserSimpleDTO. User id: " + user.getId());
        LobbyDTO.UserSimpleDTO simple = new LobbyDTO.UserSimpleDTO();
        simple.setId(user.getId());
        simple.setUsername(user.getUsername());
//        System.out.println("Converted UserSimpleDTO: " + simple);
        return simple;
    }

    private LobbyDTO toLobbyDTO(Lobbies lobby) {
//        System.out.println("Converting Lobbies to LobbyDTO. Lobby id: " + lobby.getId());
        LobbyDTO dto = new LobbyDTO();
        BeanUtils.copyProperties(lobby, dto);
        if (lobby.getHostUser() != null) {
            dto.setHostUser(toUserSimpleDTO(lobby.getHostUser()));
        }
        dto.setTeamAPlayers(MappingUtils.mapList(lobby.getTeamAPlayers(), this::toUserSimpleDTO));
        dto.setTeamBPlayers(MappingUtils.mapList(lobby.getTeamBPlayers(), this::toUserSimpleDTO));
        dto.setUnassignedPlayers(MappingUtils.mapList(lobby.getUnassignedPlayers(), this::toUserSimpleDTO));
//        System.out.println("Converted LobbyDTO: " + dto);
        return dto;
    }


}

