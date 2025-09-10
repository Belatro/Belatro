package backend.belatro.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class GameSchedulingConfig {
    @Bean("gameScheduler")
    public TaskScheduler gameScheduler() {
        ThreadPoolTaskScheduler t = new ThreadPoolTaskScheduler();
        t.setPoolSize(2);
        t.setThreadNamePrefix("game-scheduler-");
        t.initialize();
        return t;
    }
}