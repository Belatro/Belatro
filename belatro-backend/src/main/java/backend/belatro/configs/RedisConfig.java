package backend.belatro.configs;

import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.username}")
    private String redisUsername;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean redisSslEnabled;

    @Value("${spring.redis.lettuce.pool.max-active:16}")
    private int maxActive;

    @Value("${spring.redis.lettuce.pool.max-idle:8}")
    private int maxIdle;

    @Value("${spring.redis.lettuce.pool.min-idle:2}")
    private int minIdle;

    @Bean(destroyMethod = "shutdown")
    public ClientResources clientResources() {
        ClientResources resources = DefaultClientResources.create();
        logger.debug("Initialized Lettuce ClientResources");
        return resources;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(ClientResources clientResources) {
        logger.debug("Building RedisStandaloneConfiguration: host={}, port={}", redisHost, redisPort);
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);

        if (redisUsername != null && !redisUsername.isEmpty()) {
            redisConfig.setUsername(redisUsername);
            logger.debug("Using Redis username: {}", redisUsername);
        }
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
            logger.debug("Using Redis password: [PROTECTED]");
        }

        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(maxActive);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        logger.debug("Pool config: maxActive={}, maxIdle={}, minIdle={}", maxActive, maxIdle, minIdle);

        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder =
                LettucePoolingClientConfiguration.builder()
                        .poolConfig(poolConfig)
                        .commandTimeout(Duration.ofSeconds(2))
                        .clientResources(clientResources);

        if (redisSslEnabled) {
            logger.debug("Enabling SSL on Lettuce client");
            builder.useSsl();
        }

        LettucePoolingClientConfiguration lettucePoolingConfig = builder.build();
        logger.debug("Built LettucePoolingClientConfiguration");

        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfig, lettucePoolingConfig);
        connectionFactory.afterPropertiesSet();
        logger.debug("Initialized LettuceConnectionFactory");
        return connectionFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory redisConnectionFactory) {
        logger.debug("Creating RedisTemplate");
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.afterPropertiesSet();
        logger.debug("RedisTemplate created successfully");
        return template;
    }
}
