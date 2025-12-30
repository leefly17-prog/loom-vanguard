package io.github.leefly17_prog.loomvanguard.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 性能压测脚本
 * 模拟论坛通知场景：数十万粉丝需要被通知
 *
 * @author Fly
 * @since 1.0.0
 */
class PerformanceTest {

    /**
     * 模拟论坛通知场景
     * 场景：一个有 100,000 粉丝的用户发帖，需要通知所有粉丝
     */
    @Test
    @Timeout(120)
    void testForumNotificationScenario() throws InterruptedException {
        int fanCount = 100_000;
        int maxConcurrency = 1000; // 限制并发数为 1000，防止压垮下游通知服务
        
        VanguardDispatcher dispatcher = new VanguardDispatcher(maxConcurrency);
        
        AtomicInteger notifiedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(fanCount);
        
        // 模拟粉丝列表
        List<Fan> fans = createFans(fanCount);
        
        System.out.println("==========================================");
        System.out.println("开始模拟论坛通知场景");
        System.out.println("粉丝数量: " + fanCount);
        System.out.println("最大并发数: " + maxConcurrency);
        System.out.println("==========================================");
        
        long startTime = System.currentTimeMillis();
        
        // 分发通知任务
        dispatcher.dispatch(fans, fan -> {
            try {
                // 模拟发送通知（可能调用 HTTP API、消息队列等）
                sendNotification(fan);
                notifiedCount.incrementAndGet();
            } catch (Exception e) {
                failedCount.incrementAndGet();
                // 实际场景中可以记录日志或重试
            } finally {
                latch.countDown();
            }
        });
        
        // 等待所有任务完成
        boolean completed = latch.await(60, TimeUnit.SECONDS);
        
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println("==========================================");
        System.out.println("通知完成！");
        System.out.println("总耗时: " + duration + "ms (" + (duration / 1000.0) + "秒)");
        System.out.println("成功通知: " + notifiedCount.get());
        System.out.println("失败通知: " + failedCount.get());
        System.out.println("吞吐量: " + (fanCount * 1000.0 / duration) + " 通知/秒");
        System.out.println("==========================================");
        System.out.println("监控指标:");
        System.out.println(dispatcher.getMetrics());
        System.out.println("==========================================");
        
        assertTrue(completed, "所有任务应在60秒内完成");
        assertTrue(notifiedCount.get() + failedCount.get() == fanCount, 
                "所有粉丝都应被处理");
    }

    /**
     * 测试不同并发限制下的性能表现
     */
    @Test
    @Timeout(180)
    void testDifferentConcurrencyLevels() throws InterruptedException {
        int taskCount = 10_000;
        int[] concurrencyLevels = {10, 50, 100, 500, 1000};
        
        System.out.println("==========================================");
        System.out.println("测试不同并发限制下的性能");
        System.out.println("任务数量: " + taskCount);
        System.out.println("==========================================");
        
        for (int maxConcurrency : concurrencyLevels) {
            VanguardDispatcher dispatcher = new VanguardDispatcher(maxConcurrency);
            CountDownLatch latch = new CountDownLatch(taskCount);
            
            long startTime = System.currentTimeMillis();
            
            dispatcher.dispatch(createTasks(taskCount), task -> {
                try {
                    Thread.sleep(10); // 模拟任务执行时间
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                latch.countDown();
            });
            
            latch.await(30, TimeUnit.SECONDS);
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.printf("并发限制: %4d | 耗时: %6dms | 吞吐量: %.2f 任务/秒%n",
                    maxConcurrency, duration, taskCount * 1000.0 / duration);
        }
        
        System.out.println("==========================================");
    }

    /**
     * 测试背压控制效果
     * 验证信号量确实限制了并发数
     */
    @Test
    @Timeout(60)
    void testBackpressureControl() throws InterruptedException {
        int maxConcurrency = 50;
        int taskCount = 500;
        
        VanguardDispatcher dispatcher = new VanguardDispatcher(maxConcurrency);
        
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger currentConcurrent = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(taskCount);
        
        dispatcher.dispatch(createTasks(taskCount), task -> {
            int current = currentConcurrent.incrementAndGet();
            maxConcurrent.updateAndGet(max -> Math.max(max, current));
            
            try {
                Thread.sleep(100); // 模拟任务执行
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            currentConcurrent.decrementAndGet();
            latch.countDown();
        });
        
        latch.await(30, TimeUnit.SECONDS);
        
        System.out.println("==========================================");
        System.out.println("背压控制测试");
        System.out.println("最大并发限制: " + maxConcurrency);
        System.out.println("实际最大并发: " + maxConcurrent.get());
        System.out.println("==========================================");
        
        assertTrue(maxConcurrent.get() <= maxConcurrency,
                "实际并发数不应超过限制，限制: " + maxConcurrency + ", 实际: " + maxConcurrent.get());
    }

    private List<Fan> createFans(int count) {
        List<Fan> fans = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            fans.add(new Fan("fan_" + i, "user_" + i + "@example.com"));
        }
        return fans;
    }

    private void sendNotification(Fan fan) {
        // 模拟发送通知的耗时操作
        // 实际场景中可能是：
        // - HTTP 请求到通知服务
        // - 发送消息到消息队列
        // - 写入数据库
        try {
            Thread.sleep(1); // 模拟网络延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private List<Integer> createTasks(int count) {
        List<Integer> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(i);
        }
        return tasks;
    }

    /**
     * 粉丝实体类（示例）
     */
    static class Fan {
        private final String id;
        private final String email;

        public Fan(String id, String email) {
            this.id = id;
            this.email = email;
        }

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }
    }
}

