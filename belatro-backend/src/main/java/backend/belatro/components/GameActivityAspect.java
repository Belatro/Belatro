package backend.belatro.components;

import backend.belatro.pojo.gamelogic.BelotGame;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Aspect
@Component
@RequiredArgsConstructor
public class GameActivityAspect {

    private static final String KEY_PREFIX = "belot:game:";
    private final RedisTemplate<String, BelotGame> belotGameRedisTemplate;
    private static final Logger log = LoggerFactory.getLogger(GameActivityAspect.class);

    @Around("@annotation(backend.belatro.annotations.GameAction)")
    public Object stamp(ProceedingJoinPoint pjp) throws Throwable {

        Object ret = pjp.proceed();          // 1️⃣ run playCard / placeBid / …

        /* 2️⃣ get the matchId (first arg of every @GameAction method) */
        String matchId = (String) pjp.getArgs()[0];
        String redisKey = KEY_PREFIX + matchId;

        /* 3️⃣ fetch → update → save */
        BelotGame g = belotGameRedisTemplate.opsForValue().get(redisKey);
        if (g != null) {
            g.setLastActivity(Instant.now());                // bump timestamp
            belotGameRedisTemplate.opsForValue().set(redisKey, g);  // <- MUST save!
            log.debug("⏰ lastActivity stamped for {}", matchId);
        } else {
            log.warn("Aspect: no BelotGame found for key {}", redisKey);
        }
        return ret;
    }
}

