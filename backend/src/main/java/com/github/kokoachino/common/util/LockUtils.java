package com.github.kokoachino.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;


/**
 * 分布式锁工具类
 * 基于 Redis 实现的分布式锁,支持用户锁、团队锁等
 *
 * @author kokoachino
 * @date 2026-02-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LockUtils {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 锁的默认过期时间（秒）
     */
    private static final long DEFAULT_EXPIRE_TIME = 30;

    /**
     * 获取用户锁的 Key
     */
    public static String getUserLockKey(Integer userId) {
        return "lock:user:" + userId;
    }

    /**
     * 获取团队锁的 Key
     */
    public static String getTeamLockKey(Integer teamId) {
        return "lock:team:" + teamId;
    }

    /**
     * 尝试获取锁
     *
     * @param lockKey    锁的 Key
     * @param lockValue  锁的值（通常用于标识持有锁的客户端）
     * @param expireTime 过期时间（秒）
     * @return 是否成功获取锁
     */
    public boolean tryLock(String lockKey, String lockValue, long expireTime) {
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, expireTime, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }

    /**
     * 尝试获取锁（使用默认过期时间）
     */
    public boolean tryLock(String lockKey, String lockValue) {
        return tryLock(lockKey, lockValue, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 释放锁
     *
     * @param lockKey   锁的 Key
     * @param lockValue 锁的值（用于验证是否为当前客户端持有的锁）
     * @return 是否成功释放锁
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) else return 0 end";
        Long result = stringRedisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
                java.util.Collections.singletonList(lockKey),
                lockValue
        );
        return result != null && result > 0;
    }

    /**
     * 执行带锁的操作
     *
     * @param lockKey  锁的 Key
     * @param lockValue 锁的值
     * @param task     要执行的任务
     */
    public void executeWithLock(String lockKey, String lockValue, Runnable task) {
        boolean locked = tryLock(lockKey, lockValue);
        if (!locked) {
            throw new RuntimeException("获取锁失败: " + lockKey);
        }
        try {
            task.run();
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }

    /**
     * 执行带锁的操作（带返回值）
     *
     * @param lockKey  锁的 Key
     * @param lockValue 锁的值
     * @param task     要执行的任务
     * @param <T>      返回值类型
     * @return 任务执行结果
     */
    public <T> T executeWithLock(String lockKey, String lockValue, java.util.function.Supplier<T> task) {
        boolean locked = tryLock(lockKey, lockValue);
        if (!locked) {
            throw new RuntimeException("获取锁失败: " + lockKey);
        }
        try {
            return task.get();
        } finally {
            releaseLock(lockKey, lockValue);
        }
    }
}
