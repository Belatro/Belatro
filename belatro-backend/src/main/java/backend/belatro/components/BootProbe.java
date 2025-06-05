package backend.belatro.components;

import backend.belatro.pojo.gamelogic.BelotGame;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class BootProbe implements CommandLineRunner {

    private final RedisTemplate<String, BelotGame> tpl;

    public BootProbe(RedisTemplate<String, BelotGame> tpl) { this.tpl = tpl; }

    @Override public void run(String... args) {
        System.out.println("Typed RedisTemplate bean class = " + tpl.getClass());
    }
}