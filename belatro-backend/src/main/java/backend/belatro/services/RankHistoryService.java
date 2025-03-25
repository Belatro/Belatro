package backend.belatro.services;

import backend.belatro.components.EloRatingCalculator;
import backend.belatro.repos.RankHistoryRepo;
import backend.belatro.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RankHistoryService {
    private final RankHistoryRepo rankHistoryRepo;
    private final UserRepo userRepo;
    private final EloRatingCalculator eloRatingCalculator;

    @Autowired
    public RankHistoryService(RankHistoryRepo rankHistoryRepo, UserRepo userRepo,  EloRatingCalculator eloRatingCalculator) {
        this.rankHistoryRepo = rankHistoryRepo;
        this.userRepo = userRepo;
        this.eloRatingCalculator = eloRatingCalculator;
    }



}
