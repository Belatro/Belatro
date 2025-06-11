package backend.belatro.services;

import backend.belatro.repos.LobbiesRepo;
import backend.belatro.repos.MatchRepo;
import backend.belatro.repos.SkinInventoryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserService userService;
    private final LobbiesRepo lobbiesRepo;
    private final MatchRepo matchRepo;
    private final SkinInventoryRepo skinRepo;

    @Autowired
    public AdminService(UserService userService,
                        LobbiesRepo lobbiesRepo,
                        MatchRepo matchRepo,
                        SkinInventoryRepo skinRepo) {
        this.userService = userService;
        this.lobbiesRepo = lobbiesRepo;
        this.matchRepo = matchRepo;
        this.skinRepo = skinRepo;
    }

    @Transactional
    public void forgetUser(String id) {
        userService.deleteUser(id);

    }
}
