# Loom Vanguard

[![Java CI with Maven](https://github.com/leefly17-prog/loom-vanguard/actions/workflows/maven.yml/badge.svg)](https://github.com/leefly17-prog/loom-vanguard/actions/workflows/maven.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.txt)

一个基于 Java 21 虚拟线程的高性能任务分发组件，使用信号量实现背压控制，防止压垮下游服务。

## ✨ 特性

- 🚀 **虚拟线程支持**：利用 Java 21 的虚拟线程，可以瞬间拉起数十万个并发任务
- 🛡️ **背压控制**：通过信号量机制限制并发数，保护下游服务不被压垮
- 📊 **监控指标**：内置高性能监控指标（基于 LongAdder），实时统计任务执行情况
- ⚡ **高性能**：相比传统线程池，虚拟线程的创建和切换成本极低
- 🔧 **易于使用**：简洁的 API 设计，几行代码即可使用

## 📋 要求

- Java 21 或更高版本（虚拟线程需要 Java 21+）
- Maven 3.6+

## 🚀 快速开始

### Maven 依赖

#### 方式一：通过 JitPack（推荐，最简单）

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.leefly17-prog</groupId>
        <artifactId>loom-vanguard</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

**注意**：将 `leefly17-prog` 替换为你的 GitHub 用户名。

#### 方式二：通过 Maven Central（如果已发布）

```xml
<dependency>
    <groupId>io.github.leefly17-prog</groupId>
    <artifactId>loom-vanguard</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 基本使用

```java
import io.github.leefly17-prog.loomvanguard.core.VanguardDispatcher;
import java.util.List;

// 创建分发器，限制最大并发数为 100
VanguardDispatcher dispatcher = new VanguardDispatcher(100);

// 准备任务列表
List<String> tasks = List.of("task1", "task2", "task3", ...);

// 分发任务
dispatcher.dispatch(tasks, task -> {
    // 执行任务逻辑
    System.out.println("处理任务: " + task);
    // 例如：发送通知、调用 API、写入数据库等
});

// 查看监控指标
System.out.println(dispatcher.getMetrics());
```

### 论坛通知场景示例

```java
import io.github.leefly17-prog.loomvanguard.core.VanguardDispatcher;
import java.util.List;

// 场景：一个有 100,000 粉丝的用户发帖，需要通知所有粉丝
int fanCount = 100_000;
int maxConcurrency = 1000; // 限制并发数，防止压垮通知服务

VanguardDispatcher dispatcher = new VanguardDispatcher(maxConcurrency);

// 模拟粉丝列表
List<Fan> fans = getFans(fanCount);

// 分发通知任务
dispatcher.dispatch(fans, fan -> {
    try {
        // 发送通知（可能是 HTTP 请求、消息队列等）
        sendNotification(fan);
    } catch (Exception e) {
        // 处理异常
        log.error("通知发送失败", e);
    }
});

// 查看执行结果
VanguardMetrics metrics = dispatcher.getMetrics();
System.out.println("成功通知: " + metrics.getSuccess());
System.out.println("失败通知: " + metrics.getFailure());
System.out.println("平均执行时间: " + metrics.getAverageExecutionTimeMs() + "ms");
```

### 高级配置

```java
// 创建带超时和快速失败的分发器
VanguardDispatcher dispatcher = new VanguardDispatcher(
    100,        // 最大并发数
    5000,       // 获取信号量超时时间（毫秒）
    true        // 快速失败模式
);

// 获取当前可用信号量
int available = dispatcher.availablePermits();

// 获取等待队列长度
int queueLength = dispatcher.getQueueLength();
```

## 📊 监控指标

`VanguardMetrics` 提供了丰富的监控指标：

- `getSubmitted()` - 已提交的任务数
- `getAcquired()` - 已获取信号量的任务数
- `getReleased()` - 已释放信号量的任务数
- `getSuccess()` - 成功执行的任务数
- `getFailure()` - 失败的任务数
- `getTimeout()` - 超时的任务数
- `getInterrupted()` - 被中断的任务数
- `getAverageExecutionTimeMs()` - 平均执行时间（毫秒）
- `getTotalExecutionTimeMs()` - 总执行时间（毫秒）

## 🏗️ 架构设计

### 核心组件

- **VanguardDispatcher**：任务分发器，负责将任务分发到虚拟线程执行
- **VanguardMetrics**：监控指标收集器，使用 LongAdder 实现高性能并发统计
- **VanguardException**：自定义异常类

### 工作原理

1. **任务提交**：调用 `dispatch()` 方法提交任务列表
2. **虚拟线程创建**：为每个任务创建一个虚拟线程
3. **信号量控制**：虚拟线程尝试获取信号量，控制并发数
4. **任务执行**：获取到信号量后执行任务逻辑
5. **资源释放**：任务完成后释放信号量，允许其他任务执行
6. **指标收集**：在整个过程中收集各种监控指标

### 背压机制

通过 `Semaphore` 实现背压控制：
- 当并发数达到上限时，新任务会等待信号量释放
- 防止同时创建过多连接或请求，保护下游服务
- 可以通过 `availablePermits()` 和 `getQueueLength()` 监控当前状态

## 🧪 测试

运行单元测试：

```bash
mvn test
```

运行性能测试：

```bash
mvn test -Dtest=PerformanceTest
```

## 📈 性能表现

在典型的论坛通知场景中（100,000 个任务，1000 并发限制）：

- **传统线程池**：需要创建大量线程，内存占用高，上下文切换开销大
- **Loom Vanguard**：利用虚拟线程，可以瞬间拉起所有任务，通过信号量控制实际并发，性能提升显著

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📝 许可证

本项目采用 [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) 许可证。

## 📧 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 [Issue](https://github.com/leefly17-prog/loom-vanguard/issues)

---

**注意**：使用前请确保你的 Java 版本是 21 或更高版本，虚拟线程是 Java 21 引入的特性。

