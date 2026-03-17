package com.github.kokoachino.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;


/**
 * Redisson 配置
 * 显式配置超时、重试、连接池参数，提升线上 Redis 抖动时的稳定性
 *
 * @author Kokoa_Chino
 * @date 2026-03-17
 */
@Configuration
public class RedissonConfig {

    private final RedisProperties redisProperties;
    private final RedissonProperties redissonProperties;
    private final Environment environment;

    public RedissonConfig(
            RedisProperties redisProperties,
            RedissonProperties redissonProperties,
            Environment environment
    ) {
        this.redisProperties = redisProperties;
        this.redissonProperties = redissonProperties;
        this.environment = environment;
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        if (redissonProperties.getThreads() > 0) {
            config.setThreads(redissonProperties.getThreads());
        }
        if (redissonProperties.getNettyThreads() > 0) {
            config.setNettyThreads(redissonProperties.getNettyThreads());
        }
        config.setLockWatchdogTimeout(redissonProperties.getLockWatchdogTimeoutMs());
        String address = resolveRedisAddress();
        SingleServerConfig singleServer = config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisProperties.getDatabase())
                .setConnectTimeout(redissonProperties.getConnectTimeoutMs())
                .setTimeout(redissonProperties.getCommandTimeoutMs())
                .setIdleConnectionTimeout(redissonProperties.getIdleConnectionTimeoutMs())
                .setRetryAttempts(redissonProperties.getRetryAttempts())
                .setRetryInterval(redissonProperties.getRetryIntervalMs())
                .setPingConnectionInterval(redissonProperties.getPingConnectionIntervalMs())
                .setKeepAlive(redissonProperties.isKeepAlive())
                .setTcpNoDelay(redissonProperties.isTcpNoDelay())
                .setConnectionPoolSize(redissonProperties.getConnectionPoolSize())
                .setConnectionMinimumIdleSize(redissonProperties.getConnectionMinimumIdleSize())
                .setSubscriptionConnectionPoolSize(redissonProperties.getSubscriptionConnectionPoolSize())
                .setSubscriptionConnectionMinimumIdleSize(redissonProperties.getSubscriptionConnectionMinimumIdleSize());
        if (StringUtils.hasText(redisProperties.getUsername())) {
            singleServer.setUsername(redisProperties.getUsername());
        }
        if (StringUtils.hasText(redisProperties.getPassword())) {
            singleServer.setPassword(redisProperties.getPassword());
        }
        return Redisson.create(config);
    }

    private String resolveRedisAddress() {
        String redisUrl = environment.getProperty("spring.data.redis.url");
        if (StringUtils.hasText(redisUrl)) {
            if (redisUrl.startsWith("redis://") || redisUrl.startsWith("rediss://")) {
                return redisUrl;
            }
            String urlScheme = redissonProperties.isSslEnabled() ? "rediss://" : "redis://";
            return urlScheme + redisUrl;
        }
        String scheme = redissonProperties.isSslEnabled() ? "rediss://" : "redis://";
        return scheme + redisProperties.getHost() + ":" + redisProperties.getPort();
    }
}