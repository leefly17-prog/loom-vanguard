package io.github.leefly17_prog.loomvanguard.core;

import io.github.leefly17_prog.loomvanguard.exception.VanguardException;
import io.github.leefly17_prog.loomvanguard.metrics.VanguardMetrics;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 基于虚拟线程的高性能任务分发器
 * 使用信号量实现背压控制，防止压垮下游服务
 *
 * @author Fly
 * @since 1.0.0
 */
public class VanguardDispatcher {
    private final Semaphore semaphore;
    private final VanguardMetrics metrics;
    private final long acquireTimeoutMs;
    private final boolean failFast;

    /**
     * 创建分发器实例
     *
     * @param maxConcurrency 最大并发数（信号量大小）
     */
    public VanguardDispatcher(int maxConcurrency) {
        this(maxConcurrency, -1, false);
    }

    /**
     * 创建分发器实例
     *
     * @param maxConcurrency 最大并发数（信号量大小）
     * @param acquireTimeoutMs 获取信号量超时时间（毫秒），-1 表示不超时
     * @param failFast 是否快速失败（超时或中断时是否抛出异常）
     */
    public VanguardDispatcher(int maxConcurrency, long acquireTimeoutMs, boolean failFast) {
        if (maxConcurrency <= 0) {
            throw new IllegalArgumentException("maxConcurrency must be positive, got: " + maxConcurrency);
        }
        this.semaphore = new Semaphore(maxConcurrency);
        this.metrics = new VanguardMetrics();
        this.acquireTimeoutMs = acquireTimeoutMs;
        this.failFast = failFast;
    }

    /**
     * 分发任务到虚拟线程
     *
     * @param tasks 任务列表
     * @param action 执行逻辑
     * @param <T> 任务类型
     */
    public <T> void dispatch(Iterable<T> tasks, Consumer<T> action) {
        if (tasks == null) {
            throw new IllegalArgumentException("tasks cannot be null");
        }
        if (action == null) {
            throw new IllegalArgumentException("action cannot be null");
        }

        for (T task : tasks) {
            Thread.ofVirtual()
                    .name("vanguard-worker-", 0)
                    .start(() -> executeTask(task, action));
        }
    }

    /**
     * 执行单个任务
     */
    private <T> void executeTask(T task, Consumer<T> action) {
        boolean acquired = false;
        try {
            metrics.incrementSubmitted();
            
            // 尝试获取信号量
            if (acquireTimeoutMs > 0) {
                acquired = semaphore.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);
                if (!acquired) {
                    metrics.incrementTimeout();
                    if (failFast) {
                        throw new VanguardException("Failed to acquire semaphore within timeout: " + acquireTimeoutMs + "ms");
                    }
                    return;
                }
            } else {
                semaphore.acquire();
                acquired = true;
            }

            metrics.incrementAcquired();
            long startTime = System.nanoTime();
            
            try {
                action.accept(task);
                metrics.incrementSuccess();
            } catch (Exception e) {
                metrics.incrementFailure();
                // 不重新抛出异常，避免在外层 catch 中重复计数
                // 异常已经被记录，不需要传播
            } finally {
                long duration = System.nanoTime() - startTime;
                metrics.recordExecutionTime(duration);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            metrics.incrementInterrupted();
            if (failFast) {
                throw new VanguardException("Task execution interrupted", e);
            }
        } catch (Exception e) {
            // 这里只处理 VanguardException（超时异常），不处理 action 抛出的异常
            // 因为 action 的异常已经在上面处理了
            if (failFast && e instanceof VanguardException) {
                throw e;
            }
        } finally {
            if (acquired) {
                semaphore.release();
                metrics.incrementReleased();
            }
        }
    }

    /**
     * 获取当前可用信号量数量
     *
     * @return 可用信号量数量
     */
    public int availablePermits() {
        return semaphore.availablePermits();
    }

    /**
     * 获取当前正在等待的线程数（近似值）
     *
     * @return 等待线程数
     */
    public int getQueueLength() {
        return semaphore.getQueueLength();
    }

    /**
     * 获取监控指标
     *
     * @return 监控指标实例
     */
    public VanguardMetrics getMetrics() {
        return metrics;
    }
}

