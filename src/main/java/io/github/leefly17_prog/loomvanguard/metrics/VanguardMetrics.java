package io.github.leefly17_prog.loomvanguard.metrics;

import java.util.concurrent.atomic.LongAdder;

/**
 * 监控指标收集器
 * 使用 LongAdder 实现高性能的并发统计
 *
 * @author Fly
 * @since 1.0.0
 */
public class VanguardMetrics {
    private final LongAdder submitted = new LongAdder();
    private final LongAdder acquired = new LongAdder();
    private final LongAdder released = new LongAdder();
    private final LongAdder success = new LongAdder();
    private final LongAdder failure = new LongAdder();
    private final LongAdder timeout = new LongAdder();
    private final LongAdder interrupted = new LongAdder();
    private final LongAdder totalExecutionTimeNanos = new LongAdder();
    private final LongAdder executionCount = new LongAdder();

    /**
     * 增加已提交任务数
     */
    public void incrementSubmitted() {
        submitted.increment();
    }

    /**
     * 增加已获取信号量的任务数
     */
    public void incrementAcquired() {
        acquired.increment();
    }

    /**
     * 增加已释放信号量的任务数
     */
    public void incrementReleased() {
        released.increment();
    }

    /**
     * 增加成功执行的任务数
     */
    public void incrementSuccess() {
        success.increment();
    }

    /**
     * 增加失败的任务数
     */
    public void incrementFailure() {
        failure.increment();
    }

    /**
     * 增加超时的任务数
     */
    public void incrementTimeout() {
        timeout.increment();
    }

    /**
     * 增加被中断的任务数
     */
    public void incrementInterrupted() {
        interrupted.increment();
    }

    /**
     * 记录任务执行时间
     *
     * @param nanos 执行时间（纳秒）
     */
    public void recordExecutionTime(long nanos) {
        totalExecutionTimeNanos.add(nanos);
        executionCount.increment();
    }

    /**
     * 获取已提交任务数
     */
    public long getSubmitted() {
        return submitted.sum();
    }

    /**
     * 获取已获取信号量的任务数
     */
    public long getAcquired() {
        return acquired.sum();
    }

    /**
     * 获取已释放信号量的任务数
     */
    public long getReleased() {
        return released.sum();
    }

    /**
     * 获取成功执行的任务数
     */
    public long getSuccess() {
        return success.sum();
    }

    /**
     * 获取失败的任务数
     */
    public long getFailure() {
        return failure.sum();
    }

    /**
     * 获取超时的任务数
     */
    public long getTimeout() {
        return timeout.sum();
    }

    /**
     * 获取被中断的任务数
     */
    public long getInterrupted() {
        return interrupted.sum();
    }

    /**
     * 获取平均执行时间（毫秒）
     */
    public double getAverageExecutionTimeMs() {
        long count = executionCount.sum();
        if (count == 0) {
            return 0.0;
        }
        return (totalExecutionTimeNanos.sum() / 1_000_000.0) / count;
    }

    /**
     * 获取总执行时间（毫秒）
     */
    public double getTotalExecutionTimeMs() {
        return totalExecutionTimeNanos.sum() / 1_000_000.0;
    }

    /**
     * 重置所有指标
     */
    public void reset() {
        submitted.reset();
        acquired.reset();
        released.reset();
        success.reset();
        failure.reset();
        timeout.reset();
        interrupted.reset();
        totalExecutionTimeNanos.reset();
        executionCount.reset();
    }

    /**
     * 获取指标快照
     *
     * @return 指标快照字符串
     */
    @Override
    public String toString() {
        return String.format(
                "VanguardMetrics{提交=%d, 获取=%d, 释放=%d, 成功=%d, 失败=%d, 超时=%d, 中断=%d, 平均执行时间=%.2fms}",
                getSubmitted(), getAcquired(), getReleased(), getSuccess(), getFailure(),
                getTimeout(), getInterrupted(), getAverageExecutionTimeMs()
        );
    }
}

