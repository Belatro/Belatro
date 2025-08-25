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
import backend.belatro.services.UserService;
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
import java.util.stream.Stream;


@Service
public class MatchServiceImpl implements IMatchService {

    private final MatchRepo matchRepo;
    private final MatchMoveRepo matchMoveRepo;
    private final RankHistoryService rankHistoryService;
    private final UserService userService; // ← add

    private static final int PLAYS_PER_HAND  = 32;
    private static final int PLAYS_PER_TRICK = 4;
    private static final AtomicLong GLOBAL_MOVE_SEQUENCE = new AtomicLong();


    @Autowired
    public MatchServiceImpl(MatchRepo matchRepo, MatchMoveRepo matchMoveRepo, RankHistoryService rankHistoryService, UserService userService) {
        this.matchRepo = matchRepo;
        this.matchMoveRepo = matchMoveRepo;
        this.rankHistoryService = rankHistoryService;
        this.userService = userService;
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

        int lastEndNo = matchMoveRepo
                .findFirstByMatchIdAndTypeOrderByNumberDesc(matchId, MoveType.END_HAND)
                .map(MatchMove::getNumber)
                .orElse(0);

        long handsFinished = matchMoveRepo.countByMatchIdAndType(matchId, MoveType.END_HAND);
        int handNo = (int) handsFinished + 1;

        long playsSinceBoundary = matchMoveRepo
                .countByMatchIdAndTypeAndNumberGreaterThan(matchId, MoveType.PLAY_CARD, lastEndNo);

        int trickNo;
        switch (moveType) {
            case PLAY_CARD -> {
                long afterThis = playsSinceBoundary + 1;
                trickNo = (int) ((afterThis - 1) / PLAYS_PER_TRICK) + 1;
            }
            case END_TRICK -> {
                trickNo = (int) Math.max(1, (playsSinceBoundary / PLAYS_PER_TRICK));
            }
            case END_HAND -> {
                trickNo = (playsSinceBoundary == 0)
                        ? 0
                        : (int) (((playsSinceBoundary - 1) / PLAYS_PER_TRICK) + 1);
            }
            default -> {
                trickNo = (int) ((playsSinceBoundary) / PLAYS_PER_TRICK) + 1;
            }
        }
        if (moveType == MoveType.CHALLENGE
                && Boolean.TRUE.equals(payload.get("success"))
                && handsFinished > 0) {
            handNo = (int) handsFinished;
        }

        if (moveType == MoveType.PLAY_CARD) {
            var last = matchMoveRepo.findFirstByMatchIdAndTypeOrderByNumberDesc(matchId, MoveType.PLAY_CARD);
            if (last.isPresent()
                    && last.get().getType() == MoveType.PLAY_CARD
                    && Objects.equals(last.get().getPayload().get("playerId"), payload.get("playerId"))
                    && Objects.equals(last.get().getPayload().get("card"),     payload.get("card"))) {
                return; // skip duplicate
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

        var matchOpt = matchRepo.findById(matchId);
        final java.util.Set<String> teamAIds = matchOpt
                .map(m -> m.getTeamA().stream().map(User::getId).collect(java.util.stream.Collectors.toSet()))
                .orElse(java.util.Set.of());
        final java.util.Set<String> teamBIds = matchOpt
                .map(m -> m.getTeamB().stream().map(User::getId).collect(java.util.stream.Collectors.toSet()))
                .orElse(java.util.Set.of());
        record InProgress(List<MoveDTO> moves, int trickNo, String winnerId, Integer points) {}

        Map<Integer, String> violatingTeamByHand = new HashMap<>();
        for (MatchMove mv : moves) {
            if (mv.getType() == MoveType.CHALLENGE
                    && Boolean.TRUE.equals(mv.getPayload().get("success"))) {
                Object vt = mv.getPayload().get("violatingTeam"); // "A" or "B"
                if (vt instanceof String s) {
                    violatingTeamByHand.put(mv.getHandNo(), s);
                }
            }
        }
        Map<Integer, List<TrumpCallDTO>> trumpByHand   = new LinkedHashMap<>();
        Map<Integer, List<TrickDTO>>     tricksByHand  = new LinkedHashMap<>();
        Map<Integer, List<ChallengeDTO>> challByHand   = new LinkedHashMap<>();
        Map<Integer, HandSummaryDTO>     summaryByHand = new LinkedHashMap<>();
        Map<Integer, InProgress>         currentTricks = new HashMap<>();
        Map<Integer, Boolean>            trumpSeen     = new HashMap<>();
        Map<Integer, java.util.Set<String>> seenKeysByHand = new HashMap<>();
        Map<Integer, java.util.Set<String>> seenPlayersByHand = new HashMap<>();
        Map<Integer, java.util.Set<String>> passedPlayersByHand = new HashMap<>();

        Map<Integer, List<MoveDTO>> preTrumpBufferByHand = new HashMap<>();

        Map<Integer, Boolean> handClosedByChallenge = new HashMap<>();
        Map<Integer, List<MoveDTO>> spillToNextHand = new HashMap<>();


        int handIdx = 1;

        BiConsumer<Integer, InProgress> flush = (h, ip) -> {
            if (ip == null || ip.moves().isEmpty()) return;
            boolean isLastTrick = ip.trickNo() == 8;
            tricksByHand.get(h).add(new TrickDTO(
                    ip.trickNo(),
                    ip.winnerId(),
                    ip.points(),
                    List.copyOf(ip.moves()),
                    isLastTrick
            ));
            currentTricks.put(h, new InProgress(new ArrayList<>(), ip.trickNo() + 1, null, null));
            seenKeysByHand.put(h, new java.util.HashSet<>());
            seenPlayersByHand.put(h, new java.util.HashSet<>());
        };

        for (MatchMove mv : moves) {
            // Use the move's actual handNo to ensure correct hand placement
            int h = mv.getHandNo();
            
            // Keep track of the highest hand number seen for final cleanup
            if (h > handIdx) {
                handIdx = h;
            }
            preTrumpBufferByHand.computeIfAbsent(h, k -> new ArrayList<>());

            if (h > 1) {
                List<MoveDTO> spill = spillToNextHand.getOrDefault(h - 1, List.of());
                if (!spill.isEmpty()) {
                    preTrumpBufferByHand.computeIfAbsent(h, k -> new ArrayList<>()).addAll(spill);
                    spillToNextHand.put(h - 1, new ArrayList<>()); // clear
                }
            }
            currentTricks.computeIfAbsent(h, k -> new InProgress(new ArrayList<>(), 1, null, null));
            tricksByHand .computeIfAbsent(h, k -> new ArrayList<>());
            challByHand  .computeIfAbsent(h, k -> new ArrayList<>());
            trumpByHand  .computeIfAbsent(h, k -> new ArrayList<>());
            trumpSeen    .putIfAbsent(h, Boolean.FALSE);
            seenKeysByHand.putIfAbsent(h, new java.util.HashSet<>());
            seenPlayersByHand.putIfAbsent(h, new java.util.HashSet<>());
            passedPlayersByHand.computeIfAbsent(h, k -> new java.util.HashSet<>());

            InProgress ip = currentTricks.get(h);

            switch (mv.getType()) {
                case BID -> {
                    Boolean passFlag = (Boolean) mv.getPayload().get("pass");
                    String trumpVal = Boolean.TRUE.equals(passFlag)
                            ? "PASS"
                            : (mv.getPayload().get("trump") == null ? null : String.valueOf(mv.getPayload().get("trump")));

                    // Record PASS bids until a trump has been chosen, but only once per player per hand
                    if (Boolean.TRUE.equals(passFlag) && !Boolean.TRUE.equals(trumpSeen.get(h))) {
                        String pid = (String) mv.getPayload().get("playerId");
                        java.util.Set<String> passed = passedPlayersByHand.get(h);
                        if (!passed.contains(pid)) {
                            trumpByHand.get(h).add(new TrumpCallDTO(
                                    mv.getNumber(),
                                    pid,
                                    "PASS"
                            ));
                            passed.add(pid);
                        }
                    }

                    // Record only the first non-PASS trump per hand
                    if (trumpVal != null && !"PASS".equals(trumpVal) && !Boolean.TRUE.equals(trumpSeen.get(h))) {
                        trumpByHand.get(h).add(new TrumpCallDTO(
                                mv.getNumber(),
                                (String) mv.getPayload().get("playerId"),
                                trumpVal
                        ));
                        trumpSeen.put(h, Boolean.TRUE);

                        // ⬇️ Drain any pre-trump buffered plays now, with de-dupe + flush
                        var buf = preTrumpBufferByHand.get(h);
                        if (buf != null && !buf.isEmpty()) {
                            var seenKeys = seenKeysByHand.get(h);
                            var seenPlayers = seenPlayersByHand.get(h);
                            for (MoveDTO m : buf) {
                                String key = m.player() + "|" + m.card();
                                if (!seenPlayers.contains(m.player()) && !seenKeys.contains(key)) {
                                    seenPlayers.add(m.player());
                                    seenKeys.add(key);
                                    ip.moves().add(m);
                                    if (ip.moves().size() >= PLAYS_PER_TRICK) {
                                        flush.accept(h, ip);
                                        // refresh after flush
                                        ip = currentTricks.get(h);
                                        seenKeys = seenKeysByHand.get(h);
                                        seenPlayers = seenPlayersByHand.get(h);
                                    }
                                }
                            }
                            buf.clear();
                        }
                    }
                }

                case PLAY_CARD -> {
                    String pid  = (String) mv.getPayload().get("playerId");
                    String card = (String) mv.getPayload().get("card");
                    Boolean legal = (Boolean) mv.getPayload().getOrDefault("legal", Boolean.TRUE);


                    // 1) If previous hand ended with 3 cards in the last trick, use THIS card to complete it (fixes "last card sent down")
                    if (h > 1 && !Boolean.TRUE.equals(trumpSeen.get(h))) {
                        List<TrickDTO> prevTricks = tricksByHand.get(h - 1);
                        if (prevTricks != null && !prevTricks.isEmpty()) {
                            TrickDTO lastPrev = prevTricks.get(prevTricks.size() - 1);
                            if (lastPrev.moves().size() == 3) {
                                List<MoveDTO> merged = new ArrayList<>(lastPrev.moves());
                                merged.add(new MoveDTO(mv.getNumber(), pid, legal, card));
                                // replace last trick with a 4-card version (winner/points may be backfilled by END_TRICK later)
                                prevTricks.set(prevTricks.size() - 1, new TrickDTO(
                                        lastPrev.trickNo(), lastPrev.winnerId(), lastPrev.points(), List.copyOf(merged), lastPrev.lastTrickBonus()
                                ));
                                break; // consumed into previous hand; don't add to current
                            }
                        }
                    }



                    // 3) If trump not chosen yet, buffer until BID (non-PASS) arrives
                    if (!Boolean.TRUE.equals(trumpSeen.get(h))) {
                        preTrumpBufferByHand.get(h).add(new MoveDTO(mv.getNumber(), pid, legal, card));
                        break;
                    }

                    // Normal path (trump chosen): de-dupe within trick and flush on 4+
                    var seenKeys = seenKeysByHand.get(h);
                    var seenPlayers = seenPlayersByHand.get(h);
                    String key = pid + "|" + card;
                    if (!seenPlayers.contains(pid) && !seenKeys.contains(key)) {
                        seenPlayers.add(pid);
                        seenKeys.add(key);
                        ip.moves().add(new MoveDTO(mv.getNumber(), pid, legal, card));
                        if (ip.moves().size() >= PLAYS_PER_TRICK) {
                            flush.accept(h, ip);
                        }
                    }
                }


                case END_TRICK -> {
                    String winnerId = (String) mv.getPayload().get("winnerId");
                    Object ptsObj   = mv.getPayload().get("points");
                    Integer points  = (ptsObj instanceof Number n) ? n.intValue() : null;

                    // If current in-progress trick has no moves, it means we auto-flushed already.
                    // Backfill winner/points into the last flushed trick of this hand.
                    if (ip.moves().isEmpty() && !tricksByHand.get(h).isEmpty()) {
                        TrickDTO lastTr = tricksByHand.get(h).get(tricksByHand.get(h).size() - 1);
                        if (lastTr.winnerId() == null) {
                            TrickDTO patched = new TrickDTO(
                                    lastTr.trickNo(),
                                    winnerId,
                                    points,
                                    lastTr.moves(),
                                    lastTr.lastTrickBonus()
                            );
                            tricksByHand.get(h).set(tricksByHand.get(h).size() - 1, patched);
                        }
                    } else {
                        InProgress withMeta = new InProgress(ip.moves(), ip.trickNo(), winnerId, points);
                        currentTricks.put(h, withMeta);
                        // Only flush if we already have 4 plays; otherwise keep meta for when the 4th play arrives
                        if (withMeta.moves().size() >= PLAYS_PER_TRICK) {
                            flush.accept(h, withMeta);
                        }
                    }
                }

                case CHALLENGE -> {
                    boolean success = Boolean.TRUE.equals(mv.getPayload().get("success"));
                    String  pid     = (String) mv.getPayload().get("playerId");

                    challByHand.get(h).add(new ChallengeDTO(mv.getNumber(), pid, success));

                    if (success) {
                        // close any partial trick in this hand
                        flush.accept(h, ip);

                        // hard stop: no further cards belong to this hand
                        handClosedByChallenge.put(h, true);


                    }
                }



                case END_HAND -> {
                    HandSummaryDTO summary = new HandSummaryDTO(
                            (Integer) mv.getPayload().get("teamAHandPoints"),
                            (Integer) mv.getPayload().get("teamBHandPoints"),
                            (Integer) mv.getPayload().get("teamADeclPoints"),
                            (Integer) mv.getPayload().get("teamBDeclPoints"),
                            (Integer) mv.getPayload().get("teamATricksWon"),
                            (Integer) mv.getPayload().get("teamBTricksWon"),
                            (Boolean) mv.getPayload().get("padanje"),
                            (Boolean) mv.getPayload().get("capot"),
                            (Integer) mv.getPayload().get("finalScoreA"),
                            (Integer) mv.getPayload().get("finalScoreB")
                    );
                    summaryByHand.put(h, summary);
                    // Finish current hand: flush any in-progress trick
                    InProgress curr = currentTricks.get(h);
                    if (curr != null && !curr.moves().isEmpty()) {
                        if (curr.moves().size() >= PLAYS_PER_TRICK) {
                            flush.accept(h, curr);
                        } else {
                            // partial trick at hand end: emit without meta
                            tricksByHand.get(h).add(new TrickDTO(
                                    curr.trickNo(),
                                    null,
                                    null,
                                    List.copyOf(curr.moves()),
                                    Boolean.FALSE
                            ));
                            currentTricks.put(h, new InProgress(new ArrayList<>(), curr.trickNo() + 1, null, null));
                        }
                    }
                    var buf = preTrumpBufferByHand.get(h);
                    if (buf != null && !buf.isEmpty()) {
                        tricksByHand.get(h).add(new TrickDTO(
                                currentTricks.get(h).trickNo(),
                                null, null,
                                List.copyOf(buf),
                                Boolean.FALSE
                        ));
                        buf.clear();
                    }
                }

                default -> { /* ignore */ }
            }
        }

        // Flush only the current hand tail
        InProgress tail = currentTricks.get(handIdx);
        if (tail != null && !tail.moves().isEmpty()) {
            if (tail.moves().size() >= PLAYS_PER_TRICK) {
                // normal complete trick
                flush.accept(handIdx, tail);
            } else {
                // partial trick: emit without winner/points and without lastTrick bonus
                tricksByHand.get(handIdx).add(new TrickDTO(
                        tail.trickNo(),
                        null,
                        null,
                        List.copyOf(tail.moves()),
                        Boolean.FALSE
                ));
            }
        }

        return tricksByHand.entrySet().stream()
                .map(e -> new HandDTO(
                        e.getKey(),
                        trumpByHand .getOrDefault(e.getKey(), List.of()),
                        tricksByHand.get(e.getKey()),
                        challByHand .getOrDefault(e.getKey(), List.of()),
                        summaryByHand.get(e.getKey())
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
            matchRepo.save(match);

            if (match.getGameMode() == GameMode.RANKED) {
                rankHistoryService.updateRatingsForMatch(match);
            }


            Set<String> participantIds = Stream.concat(
                            Optional.ofNullable(match.getTeamA()).orElseGet(List::of).stream(),
                            Optional.ofNullable(match.getTeamB()).orElseGet(List::of).stream()
                    )
                    .filter(Objects::nonNull)
                    .map(User::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (String uid : participantIds) {
                userService.findById(uid).ifPresent(u -> {
                    // increment in-memory
                    int current = u.getGamesPlayed();
                    u.setGamesPlayed(current + 1);


                    UserUpdateDTO upd = new UserUpdateDTO();
                    upd.setUsername(u.getUsername());
                    upd.setEmail(u.getEmail());
                    upd.setPasswordHashed(u.getPasswordHashed());
                    upd.setEloRating(u.getEloRating());
                    upd.setLevel(u.getLevel());
                    upd.setExpPoints(u.getExpPoints());
                    upd.setLastLogin(u.getLastLogin());
                    upd.setGamesPlayed(u.getGamesPlayed());

                    userService.updateUser(u.getId(), upd);
                });
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
                (Boolean) mm.getPayload().getOrDefault("legal", Boolean.TRUE),
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

