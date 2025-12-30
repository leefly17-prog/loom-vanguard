package io.github.leefly17_prog.loomvanguard.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VanguardMetrics 单元测试
 *
 * @author Fly
 * @since 1.0.0
 */
class VanguardMetricsTest {

    private VanguardMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new VanguardMetrics();
    }

    @Test
    void testInitialState() {
        assertEquals(0, metrics.getSubmitted());
        assertEquals(0, metrics.getAcquired());
        assertEquals(0, metrics.getSuccess());
        assertEquals(0, metrics.getFailure());
        assertEquals(0.0, metrics.getAverageExecutionTimeMs());
    }

    @Test
    void testIncrementMetrics() {
        metrics.incrementSubmitted();
        metrics.incrementAcquired();
        metrics.incrementSuccess();
        
        assertEquals(1, metrics.getSubmitted());
        assertEquals(1, metrics.getAcquired());
        assertEquals(1, metrics.getSuccess());
    }

    @Test
    void testExecutionTime() {
        metrics.recordExecutionTime(1_000_000); // 1ms
        metrics.recordExecutionTime(2_000_000); // 2ms
        metrics.recordExecutionTime(3_000_000); // 3ms
        
        assertEquals(2.0, metrics.getAverageExecutionTimeMs(), 0.01);
        assertEquals(6.0, metrics.getTotalExecutionTimeMs(), 0.01);
    }

    @Test
    void testReset() {
        metrics.incrementSubmitted();
        metrics.incrementSuccess();
        metrics.recordExecutionTime(1_000_000);
        
        metrics.reset();
        
        assertEquals(0, metrics.getSubmitted());
        assertEquals(0, metrics.getSuccess());
        assertEquals(0.0, metrics.getAverageExecutionTimeMs());
    }

    @Test
    void testConcurrentIncrement() throws InterruptedException {
        int threadCount = 10;
        int incrementsPerThread = 100;
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    metrics.incrementSubmitted();
                    metrics.incrementSuccess();
                }
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        assertEquals(threadCount * incrementsPerThread, metrics.getSubmitted());
        assertEquals(threadCount * incrementsPerThread, metrics.getSuccess());
    }

    @Test
    void testToString() {
        metrics.incrementSubmitted();
        metrics.incrementAcquired();
        metrics.incrementSuccess();
        metrics.recordExecutionTime(1_000_000);
        
        String str = metrics.toString();
        assertNotNull(str);
        assertTrue(str.contains("提交"));
        assertTrue(str.contains("成功"));
    }
}

