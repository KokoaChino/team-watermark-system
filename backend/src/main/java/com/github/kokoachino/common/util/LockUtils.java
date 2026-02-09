package com.github.kokoachino.common.util;

import com.github.kokoachino.common.exception.BizException;
import com.github.kokoachino.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


/**
 * 分布式锁工具类
 * 基于 Redisson 实现,支持用户锁、团队锁等
 *
 * @author kokoachino
 * @date 2026-02-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LockUtils {

    private final RedissonClient redissonClient;

    /**
     * 锁的默认等待时间（秒）
     */
    private static final long DEFAULT_WAIT_TIME = 3;

    /**
     * 锁的默认持有时间（秒）
     */
    private static final long DEFAULT_LEASE_TIME = 30;

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
     * 执行带锁的操作
     *
     * @param lockKey 锁的 Key
     * @param task    要执行的任务
     */
    public void executeWithLock(String lockKey, Runnable task) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
            }
            task.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 执行带锁的操作（带返回值）
     *
     * @param lockKey 锁的 Key
     * @param task    要执行的任务
     * @param <T>     返回值类型
     * @return 任务执行结果
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean locked = lock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
            }
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ResultCode.LOCK_ACQUIRE_FAILED);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
