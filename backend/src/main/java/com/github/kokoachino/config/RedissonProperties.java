package com.github.kokoachino.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * Redisson 客户端参数配置
 *
 * @author Kokoa_Chino
 * @date 2026-03-17
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.redisson")
public class RedissonProperties {

    /**
     * 是否启用 SSL（rediss://）
     */
    private boolean sslEnabled;

    /**
     * 连接超时（毫秒）
     */
    private int connectTimeoutMs;

    /**
     * Redis 命令超时（毫秒）
     */
    private int commandTimeoutMs;

    /**
     * 空闲连接超时（毫秒）
     */
    private int idleConnectionTimeoutMs;

    /**
     * 重试次数
     */
    private int retryAttempts;

    /**
     * 重试间隔（毫秒）
     */
    private int retryIntervalMs;

    /**
     * 心跳检测间隔（毫秒）
     */
    private int pingConnectionIntervalMs;

    /**
     * TCP KeepAlive
     */
    private boolean keepAlive;

    /**
     * TCP NoDelay
     */
    private boolean tcpNoDelay;

    /**
     * 普通连接池大小
     */
    private int connectionPoolSize;

    /**
     * 普通连接池最小空闲连接
     */
    private int connectionMinimumIdleSize;

    /**
     * 订阅连接池大小
     */
    private int subscriptionConnectionPoolSize;

    /**
     * 订阅连接池最小空闲连接
     */
    private int subscriptionConnectionMinimumIdleSize;

    /**
     * Redisson 工作线程数
     */
    private int threads;

    /**
     * Netty 线程数
     */
    private int nettyThreads;

    /**
     * 锁看门狗超时（毫秒）
     */
    private long lockWatchdogTimeoutMs;
}