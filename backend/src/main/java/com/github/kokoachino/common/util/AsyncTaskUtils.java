package com.github.kokoachino.common.util;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import java.util.concurrent.*;


/**
 * 异步任务工具类
 * 提供通用的异步任务执行能力
 *
 * @author Kokoa_Chino
 * @date 2026-02-10
 */
@Slf4j
@Component
public class AsyncTaskUtils {

    /**
     * 通用异步任务线程池
     * 核心线程数：4，最大线程数：10，队列容量：200
     * 拒绝策略：AbortPolicy（快速失败）
     */
    private final ThreadPoolExecutor executor;

    public AsyncTaskUtils() {
        this.executor = new ThreadPoolExecutor(
                4,
                10,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                new ThreadFactory() {
                    private int count = 0;
                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        return new Thread(r, "async-task-thread-" + (++count));
                    }
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    /**
     * 异步执行任务
     *
     * @param task 任务
     * @throws RejectedExecutionException 当线程池和队列都满时抛出
     */
    public void execute(Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("异步任务执行失败", e);
            }
        });
    }

    /**
     * 提交带返回值的异步任务
     *
     * @param task 任务
     * @param <T>  返回值类型
     * @return Future
     * @throws RejectedExecutionException 当线程池和队列都满时抛出
     */
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    /**
     * 获取线程池状态信息（用于监控）
     */
    public ThreadPoolStats getStats() {
        return new ThreadPoolStats(
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount()
        );
    }

    /**
     * 线程池统计信息
     */
    public record ThreadPoolStats(
            int activeCount,
            int poolSize,
            int corePoolSize,
            int maximumPoolSize,
            int queueSize,
            long completedTaskCount
    ) {}

    /**
     * 优雅关闭线程池
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭异步任务线程池...");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("异步任务线程池已关闭");
    }
}
