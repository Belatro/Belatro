package backend.belatro.components;

import backend.belatro.models.MatchmakingQueueEntry;
import backend.belatro.repos.MatchmakingQueueRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import static backend.belatro.configs.SecurityConfig.log;

@Component
public class QueueCache {

    private final MatchmakingQueueRepo repo;

    @Autowired
    public QueueCache(MatchmakingQueueRepo repo) { this.repo = repo; }

    /** Compare first by rating, then by queuedAt for stability. */
    private final Comparator<MatchmakingQueueEntry> cmp =
            Comparator.comparingInt(MatchmakingQueueEntry::getEloSnapshot)
                    .thenComparing(MatchmakingQueueEntry::getQueuedAt);

    private final ConcurrentSkipListSet<MatchmakingQueueEntry> set =
            new ConcurrentSkipListSet<>(cmp);

    public void add(MatchmakingQueueEntry e)   { set.add(e); }
    public void remove(MatchmakingQueueEntry e){ set.remove(e); }

    public List<MatchmakingQueueEntry> snapshot() {
        return new ArrayList<>(set);   // safe copy
    }

    /** Called on application startup to hydrate cache from Mongo */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpFromDb() {               // no extra args
        repo.findByStatus(MatchmakingQueueEntry.Status.QUEUED)
                .forEach(set::add);
        log.info("QueueCache warmed: {} entries", set.size());
    }
}
