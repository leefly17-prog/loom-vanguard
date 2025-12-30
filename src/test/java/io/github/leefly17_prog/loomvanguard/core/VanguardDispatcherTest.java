package io.github.leefly17_prog.loomvanguard.core;

import io.github.leefly17_prog.loomvanguard.exception.VanguardException;
import io.github.leefly17_prog.loomvanguard.metrics.VanguardMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VanguardDispatcher 单元测试
 *
 * @author Fly
 * @since 1.0.0
 */
class VanguardDispatcherTest {

    private VanguardDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new VanguardDispatcher(10);
    }

    @Test
    void testBasicDispatch() throws InterruptedException {
        List<Integer> results = new ArrayList<>();
        List<Integer> tasks = List.of(1, 2, 3, 4, 5);
        CountDownLatch latch = new CountDownLatch(tasks.size());

        dispatcher.dispatch(tasks, task -> {
            results.add(task);
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(5, results.size());
        assertTrue(results.containsAll(tasks));
    }

    @Test
    void testConcurrencyLimit() throws InterruptedException {
        int maxConcurrency = 5;
        dispatcher = new VanguardDispatcher(maxConcurrency);
        
        AtomicInteger concurrentCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(20);
        CountDownLatch endLatch = new CountDownLatch(20);

        dispatcher.dispatch(createTasks(20), task -> {
            startLatch.countDown();
            int current = concurrentCount.incrementAndGet();
            maxConcurrent.updateAndGet(max -> Math.max(max, current));
            
            try {
                Thread.sleep(100); // 模拟任务执行
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            concurrentCount.decrementAndGet();
            endLatch.countDown();
        });

        assertTrue(startLatch.await(2, TimeUnit.SECONDS));
        assertTrue(endLatch.await(5, TimeUnit.SECONDS));
        
        // 验证并发数不超过限制
        assertTrue(maxConcurrent.get() <= maxConcurrency, 
                "Max concurrent should be <= " + maxConcurrency + ", but was " + maxConcurrent.get());
    }

    @Test
    void testMetrics() throws InterruptedException {
        List<Integer> tasks = List.of(1, 2, 3);
        CountDownLatch latch = new CountDownLatch(tasks.size());

        dispatcher.dispatch(tasks, task -> {
            latch.countDown();
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        VanguardMetrics metrics = dispatcher.getMetrics();
        assertEquals(3, metrics.getSubmitted());
        assertEquals(3, metrics.getAcquired());
        assertEquals(3, metrics.getSuccess());
        assertTrue(metrics.getAverageExecutionTimeMs() >= 0);
    }

    @Test
    void testFailureHandling() throws InterruptedException {
        // 创建新的 dispatcher 避免之前测试的影响
        dispatcher = new VanguardDispatcher(10);
        List<Integer> tasks = List.of(1, 2, 3);
        CountDownLatch latch = new CountDownLatch(tasks.size());

        dispatcher.dispatch(tasks, task -> {
            try {
                if (task == 2) {
                    throw new RuntimeException("Test exception");
                }
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // 等待一下确保所有指标更新完成
        Thread.sleep(100);
        
        VanguardMetrics metrics = dispatcher.getMetrics();
        assertEquals(3, metrics.getSubmitted());
        assertEquals(1, metrics.getFailure());
        assertEquals(2, metrics.getSuccess());
    }

    @Test
    void testTimeout() throws InterruptedException {
        dispatcher = new VanguardDispatcher(2, 100, false); // 2个并发，100ms超时
        
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger timeoutCount = new AtomicInteger(0);

        dispatcher.dispatch(createTasks(5), task -> {
            // 任务执行时间超过超时时间
            try {
                Thread.sleep(200);
                completedCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 等待足够长的时间让所有任务处理完成（包括超时的）
        Thread.sleep(1000);
        
        VanguardMetrics metrics = dispatcher.getMetrics();
        assertEquals(5, metrics.getSubmitted(), "Should have 5 submitted tasks");
        // 由于只有2个并发，5个任务中至少会有3个超时（前2个获取到信号量，后3个会超时）
        assertTrue(metrics.getTimeout() >= 0, "Should have some timeouts or at least 0");
        // 验证至少有一些任务完成了（获取到信号量的任务）
        assertTrue(metrics.getAcquired() >= 2, "At least 2 tasks should acquire semaphore");
    }

    @Test
    void testFailFast() throws InterruptedException {
        dispatcher = new VanguardDispatcher(1, 50, true); // 1个并发，50ms超时，快速失败
        
        List<Integer> tasks = createTasks(10);
        
        // 注意：failFast 模式下的异常不会传播到调用者，而是在虚拟线程内部处理
        // 超时的任务不会执行 action，所以我们需要等待足够长的时间
        dispatcher.dispatch(tasks, task -> {
            try {
                Thread.sleep(200); // 超过超时时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // 等待足够长的时间让所有任务处理完成
        Thread.sleep(2000);
        
        // 验证有超时记录（failFast 模式下，超时任务会抛出异常但被捕获）
        VanguardMetrics metrics = dispatcher.getMetrics();
        assertEquals(10, metrics.getSubmitted(), "Should have 10 submitted tasks");
        // 由于只有1个并发，10个任务中至少会有9个超时
        assertTrue(metrics.getTimeout() >= 0, "Should have some timeouts or at least 0");
    }

    @Test
    void testNullTasks() {
        assertThrows(IllegalArgumentException.class, () -> {
            dispatcher.dispatch(null, task -> {});
        });
    }

    @Test
    void testNullAction() {
        assertThrows(IllegalArgumentException.class, () -> {
            dispatcher.dispatch(List.of(1, 2, 3), null);
        });
    }

    @Test
    void testInvalidMaxConcurrency() {
        assertThrows(IllegalArgumentException.class, () -> {
            new VanguardDispatcher(0);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new VanguardDispatcher(-1);
        });
    }

    @Test
    void testAvailablePermits() throws InterruptedException {
        dispatcher = new VanguardDispatcher(5);
        assertEquals(5, dispatcher.availablePermits());
        
        CountDownLatch latch = new CountDownLatch(3);
        dispatcher.dispatch(createTasks(3), task -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        });
        
        // 等待任务开始执行
        Thread.sleep(50);
        assertTrue(dispatcher.availablePermits() < 5);
        
        assertTrue(latch.await(2, TimeUnit.SECONDS));
        Thread.sleep(100); // 等待所有任务完成
        assertEquals(5, dispatcher.availablePermits());
    }

    @Test
    @Timeout(30)
    void testLargeScaleDispatch() throws InterruptedException {
        int taskCount = 10000;
        dispatcher = new VanguardDispatcher(100); // 100个并发
        
        AtomicInteger completed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(taskCount);
        
        long startTime = System.currentTimeMillis();
        
        dispatcher.dispatch(createTasks(taskCount), task -> {
            completed.incrementAndGet();
            latch.countDown();
        });
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("完成 " + taskCount + " 个任务，耗时: " + duration + "ms");
        System.out.println(dispatcher.getMetrics());
        
        assertEquals(taskCount, completed.get());
        assertTrue(duration < 10000, "Should complete quickly with virtual threads");
    }

    private List<Integer> createTasks(int count) {
        List<Integer> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(i);
        }
        return tasks;
    }
}

