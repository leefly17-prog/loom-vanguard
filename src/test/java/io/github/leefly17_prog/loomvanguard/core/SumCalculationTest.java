package io.github.leefly17_prog.loomvanguard.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 使用虚拟线程计算 1 到 10000000 的和
 * 演示如何使用 Loom Vanguard 组件进行并行计算
 *
 * @author Fly
 * @since 1.0.0
 */
class SumCalculationTest {

    /**
     * 使用 1000 个虚拟线程计算 1 到 10000000 的和
     */
    @Test
    @Timeout(60)
    void testSumCalculationWithVirtualThreads() throws InterruptedException {
        long targetSum = 10000000L;
        int maxConcurrency = 1000; // 使用 1000 个虚拟线程
        
        // 创建分发器，限制最大并发数为 1000
        VanguardDispatcher dispatcher = new VanguardDispatcher(maxConcurrency);
        
        // 用于存储每个任务的计算结果
        AtomicLong totalSum = new AtomicLong(0);
        
        // 将 1 到 10000000 分成 1000 个任务
        int taskCount = 1000;
        long numbersPerTask = targetSum / taskCount;
        
        System.out.println("==========================================");
        System.out.println("使用虚拟线程计算 1 到 " + targetSum + " 的和");
        System.out.println("任务数量: " + taskCount);
        System.out.println("每个任务处理: " + numbersPerTask + " 个数字");
        System.out.println("最大并发数: " + maxConcurrency);
        System.out.println("==========================================");
        
        // 创建任务列表
        List<SumTask> tasks = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            long start = i * numbersPerTask + 1;
            long end = (i == taskCount - 1) ? targetSum : (i + 1) * numbersPerTask;
            tasks.add(new SumTask(i, start, end));
        }
        
        // 使用 CountDownLatch 等待所有任务完成
        CountDownLatch latch = new CountDownLatch(taskCount);
        
        long startTime = System.currentTimeMillis();
        
        // 使用虚拟线程分发任务
        dispatcher.dispatch(tasks, task -> {
            try {
                long sum = task.calculate();
                totalSum.addAndGet(sum);
            } finally {
                latch.countDown();
            }
        });
        
        // 等待所有任务完成
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        
        long duration = System.currentTimeMillis() - startTime;
        
        // 验证结果（1 到 n 的和 = n * (n + 1) / 2）
        long expectedSum = targetSum * (targetSum + 1) / 2;
        long actualSum = totalSum.get();
        
        System.out.println("==========================================");
        System.out.println("计算完成！");
        System.out.println("总耗时: " + duration + "ms (" + (duration / 1000.0) + "秒)");
        System.out.println("预期结果: " + expectedSum);
        System.out.println("实际结果: " + actualSum);
        System.out.println("结果正确: " + (expectedSum == actualSum));
        System.out.println("==========================================");
        System.out.println("监控指标:");
        System.out.println(dispatcher.getMetrics());
        System.out.println("==========================================");
        
        // 验证所有任务都已完成
        assertTrue(completed, "所有任务应在30秒内完成");
        
        // 验证结果
        assertEquals(expectedSum, actualSum, "计算结果应该正确");
        
        // 验证所有任务都已被提交和处理
        assertEquals(taskCount, dispatcher.getMetrics().getSubmitted(), 
                "所有任务都应该被提交");
        assertEquals(taskCount, dispatcher.getMetrics().getSuccess(), 
                "所有任务都应该成功完成");
    }

    /**
     * 计算任务类
     */
    static class SumTask {
        private final int taskId;
        private final long start;
        private final long end;

        public SumTask(int taskId, long start, long end) {
            this.taskId = taskId;
            this.start = start;
            this.end = end;
        }

        /**
         * 计算从 start 到 end 的和
         */
        public long calculate() {
            long sum = 0;
            for (long i = start; i <= end; i++) {
                sum += i;
            }
            return sum;
        }

        public int getTaskId() {
            return taskId;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        @Override
        public String toString() {
            return "SumTask{id=" + taskId + ", range=[" + start + ", " + end + "]}";
        }
    }
}

