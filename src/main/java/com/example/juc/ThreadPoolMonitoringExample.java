package com.example.juc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池监控与调优示例
 * <p>
 * 演示如何监控线程池状态以及进行调优
 * </p>
 *
 * <h3>监控指标：</h3>
 * <ul>
 *   <li>核心线程数</li>
 *   <li>最大线程数</li>
 *   <li>当前线程数</li>
 *   <li>活跃线程数</li>
 *   <li>队列大小</li>
 *   <li>已完成的任务数</li>
 *   <li>拒绝的任务数</li>
 * </ul>
 *
 * <h3>调优策略：</h3>
 * <ul>
 *   <li>CPU 密集型：核心线程数 = CPU 核心数 + 1</li>
 *   <li>IO 密集型：核心线程数 = CPU 核心数 * 2</li>
 *   <li>根据实际情况调整参数</li>
 *   <li>监控各项指标，动态调整</li>
 * </ul>
 */
public class ThreadPoolMonitoringExample {

    /**
     * 场景1：基础线程池监控
     * <p>
     * 演示如何获取线程池的基本状态信息
     * </p>
     */
    public static void basicMonitoring() throws InterruptedException {
        System.out.println("=== 场景1：基础线程池监控 ===");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,                          // 核心线程数
                5,                          // 最大线程数
                60, TimeUnit.SECONDS,       // 空闲线程存活时间
                new ArrayBlockingQueue<>(3) // 任务队列
        );

        printThreadPoolStatus(executor, "初始状态");

        // 提交一些任务
        for (int i = 0; i < 8; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(1000);
                    System.out.println("任务 " + taskId + " 完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        printThreadPoolStatus(executor, "提交8个任务后");

        Thread.sleep(500);
        printThreadPoolStatus(executor, "500ms后");

        Thread.sleep(500);
        printThreadPoolStatus(executor, "1s后");

        Thread.sleep(2000);
        printThreadPoolStatus(executor, "3s后（所有任务完成）");

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * 场景2：自定义拒绝策略并统计
     * <p>
     * 演示如何实现自定义拒绝策略并统计拒绝次数
     * </p>
     */
    public static void customRejectionPolicy() throws InterruptedException {
        System.out.println("\n=== 场景2：自定义拒绝策略 ===");

        class MonitoredRejectedExecutionHandler implements RejectedExecutionHandler {
            private final AtomicInteger rejectedCount = new AtomicInteger(0);

            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                int count = rejectedCount.incrementAndGet();
                System.out.println("任务被拒绝！拒绝计数: " + count +
                        ", 队列大小: " + executor.getQueue().size() +
                        ", 活跃线程: " + executor.getActiveCount());

                // 记录拒绝的任务
                System.out.println("  拒绝的任务: " + r.toString());

                // 可以选择：1. 丢弃 2. 由调用者线程执行 3. 记录并重试
            }

            public int getRejectedCount() {
                return rejectedCount.get();
            }
        }

        MonitoredRejectedExecutionHandler handler = new MonitoredRejectedExecutionHandler();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,
                3,
                60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                handler
        );

        // 提交超过容量的任务
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            try {
                executor.submit(() -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                System.out.println("任务 " + taskId + " 已提交");
            } catch (RejectedExecutionException e) {
                System.out.println("任务 " + taskId + " 提交失败（异常）");
            }
        }

        printThreadPoolStatus(executor, "最终状态");
        System.out.println("拒绝任务总数: " + handler.getRejectedCount());

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * 场景3：持续监控线程池
     * <p>
     * 演示如何在后台持续监控线程池状态
     * </p>
     */
    public static void continuousMonitoring() throws InterruptedException {
        System.out.println("\n=== 场景3：持续监控 ===");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 5,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );

        // 启动监控线程
        Thread monitorThread = new Thread(() -> {
            int count = 0;
            while (!executor.isTerminated() && count < 20) {
                printThreadPoolStatus(executor, "监控 #" + (++count));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();

        // 提交任务
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1500));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            Thread.sleep(200);
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        printThreadPoolStatus(executor, "最终状态");
    }

    /**
     * 场景4：动态调整线程池参数
     * <p>
     * 演示如何根据监控结果动态调整线程池大小
     * </p>
     */
    public static void dynamicThreadPoolAdjustment() throws InterruptedException {
        System.out.println("\n=== 场景4：动态调整线程池 ===");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 2,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );

        printThreadPoolStatus(executor, "初始状态");

        // 提交任务
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(100);
        printThreadPoolStatus(executor, "提交5个任务后（队列积压）");

        // 动态调整核心线程数和最大线程数
        System.out.println("\n动态调整：增加线程池大小");
        executor.setCorePoolSize(5);
        executor.setMaximumPoolSize(5);

        printThreadPoolStatus(executor, "调整后状态");

        Thread.sleep(500);
        printThreadPoolStatus(executor, "500ms后");

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * 场景5：线程池告警机制
     * <p>
     * 演示如何实现线程池的告警机制
     * </p>
     */
    public static void threadPoolAlerting() throws InterruptedException {
        System.out.println("\n=== 场景5：线程池告警 ===");

        class ThreadPoolMonitor {
            private final ThreadPoolExecutor executor;
            private final double queueUsageThreshold;
            private final double activeThreadThreshold;

            public ThreadPoolMonitor(ThreadPoolExecutor executor,
                                    double queueUsageThreshold,
                                    double activeThreadThreshold) {
                this.executor = executor;
                this.queueUsageThreshold = queueUsageThreshold;
                this.activeThreadThreshold = activeThreadThreshold;
            }

            public void monitor() {
                int queueSize = executor.getQueue().size();
                int queueCapacity = getQueueCapacity();
                int activeCount = executor.getActiveCount();
                int maxPoolSize = executor.getMaximumPoolSize();

                // 检查队列使用率
                if (queueCapacity > 0) {
                    double queueUsage = (double) queueSize / queueCapacity;
                    if (queueUsage > queueUsageThreshold) {
                        System.out.println("【告警】队列使用率过高: " +
                                String.format("%.1f%%", queueUsage * 100) +
                                " (" + queueSize + "/" + queueCapacity + ")");
                    }
                }

                // 检查活跃线程率
                double activeThreadUsage = (double) activeCount / maxPoolSize;
                if (activeThreadUsage > activeThreadThreshold) {
                    System.out.println("【告警】活跃线程率过高: " +
                            String.format("%.1f%%", activeThreadUsage * 100) +
                            " (" + activeCount + "/" + maxPoolSize + ")");
                }
            }

            private int getQueueCapacity() {
                BlockingQueue<?> queue = executor.getQueue();
                if (queue instanceof ArrayBlockingQueue) {
                    return ((ArrayBlockingQueue<?>) queue).remainingCapacity() + queue.size();
                }
                return -1; // 无界队列
            }
        }

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 3,
                60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(5)
        );

        ThreadPoolMonitor monitor = new ThreadPoolMonitor(executor, 0.7, 0.8);

        // 提交任务
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            monitor.monitor();
            printThreadPoolStatus(executor, "任务 " + taskId + " 提交后");
            Thread.sleep(100);
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * 场景6：任务执行时间统计
     * <p>
     * 演示如何统计任务的执行时间
     * </p>
     */
    public static void taskExecutionTimeStatistics() throws InterruptedException {
        System.out.println("\n=== 场景6：任务执行时间统计 ===");

        class TimedThreadPoolExecutor extends ThreadPoolExecutor {
            private final ConcurrentHashMap<String, Long> taskTimes = new ConcurrentHashMap<>();

            public TimedThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                                          long keepAliveTime, TimeUnit unit,
                                          BlockingQueue<Runnable> workQueue) {
                super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
            }

            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                taskTimes.put(r.toString(), System.nanoTime());
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                Long startTime = taskTimes.remove(r.toString());
                if (startTime != null) {
                    long elapsed = System.nanoTime() - startTime;
                    System.out.println("任务执行时间: " +
                            TimeUnit.NANOSECONDS.toMillis(elapsed) + "ms");
                }
            }
        }

        TimedThreadPoolExecutor executor = new TimedThreadPoolExecutor(
                2, 2,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );

        // 提交不同执行时间的任务
        executor.submit(() -> sleep(500));
        executor.submit(() -> sleep(1000));
        executor.submit(() -> sleep(300));
        executor.submit(() -> sleep(700));

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    /**
     * 场景7：线程池健康检查
     * <p>
     * 演示如何实现线程池的健康检查
     * </p>
     */
    public static void threadPoolHealthCheck() throws InterruptedException {
        System.out.println("\n=== 场景7：线程池健康检查 ===");

        class ThreadPoolHealthChecker {
            private final ThreadPoolExecutor executor;

            public ThreadPoolHealthChecker(ThreadPoolExecutor executor) {
                this.executor = executor;
            }

            public HealthStatus checkHealth() {
                HealthStatus status = new HealthStatus();
                status.activeThreads = executor.getActiveCount();
                status.poolSize = executor.getPoolSize();
                status.queueSize = executor.getQueue().size();
                status.completedTasks = executor.getCompletedTaskCount();
                status.largestPoolSize = executor.getLargestPoolSize();
                status.isShutdown = executor.isShutdown();
                status.isTerminated = executor.isTerminated();
                status.isTerminating = executor.isTerminating();

                // 简单的健康判断
                if (status.isTerminated) {
                    status.health = "TERMINATED";
                } else if (status.isShutdown) {
                    status.health = "SHUTDOWN";
                } else if (status.activeThreads == executor.getMaximumPoolSize() &&
                           status.queueSize > 10) {
                    status.health = "OVERLOADED";
                } else {
                    status.health = "HEALTHY";
                }

                return status;
            }

            static class HealthStatus {
                int activeThreads;
                int poolSize;
                int queueSize;
                long completedTasks;
                int largestPoolSize;
                boolean isShutdown;
                boolean isTerminated;
                boolean isTerminating;
                String health;

                @Override
                public String toString() {
                    return String.format(
                            "HealthStatus{health='%s', activeThreads=%d, poolSize=%d, " +
                            "queueSize=%d, completedTasks=%d, largestPoolSize=%d}",
                            health, activeThreads, poolSize, queueSize, completedTasks, largestPoolSize
                    );
                }
            }
        }

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 4,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(5)
        );

        ThreadPoolHealthChecker checker = new ThreadPoolHealthChecker(executor);

        System.out.println("初始健康状态: " + checker.checkHealth());

        // 提交任务
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            System.out.println("任务 " + taskId + " 提交后: " + checker.checkHealth());
            Thread.sleep(100);
        }

        Thread.sleep(500);
        System.out.println("\n健康状态: " + checker.checkHealth());

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("关闭后: " + checker.checkHealth());
    }

    /**
     * 场景8：优雅关闭线程池
     * <p>
     * 演示如何优雅地关闭线程池
     * </p>
     */
    public static void gracefulShutdown() throws InterruptedException {
        System.out.println("\n=== 场景8：优雅关闭 ===");

        class GracefulShutdown {
            private final ThreadPoolExecutor executor;

            public GracefulShutdown(ThreadPoolExecutor executor) {
                this.executor = executor;
            }

            public void shutdown(long timeout, TimeUnit unit) throws InterruptedException {
                System.out.println("开始优雅关闭...");
                printThreadPoolStatus(executor, "关闭前");

                // 停止接受新任务
                executor.shutdown();

                // 等待现有任务完成
                if (!executor.awaitTermination(timeout, unit)) {
                    System.out.println("超时，强制关闭...");
                    printThreadPoolStatus(executor, "强制关闭前");

                    List<Runnable> remaining = executor.shutdownNow();
                    System.out.println("取消 " + remaining.size() + " 个未执行的任务");

                    // 再次等待
                    if (!executor.awaitTermination(timeout, unit)) {
                        System.err.println("线程池无法关闭！");
                    }
                }

                printThreadPoolStatus(executor, "关闭后");
            }
        }

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 4,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );

        // 提交长时间运行的任务
        for (int i = 0; i < 6; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("任务 " + taskId + " 开始");
                    Thread.sleep(1000 + taskId * 500);
                    System.out.println("任务 " + taskId + " 完成");
                } catch (InterruptedException e) {
                    System.out.println("任务 " + taskId + " 被中断");
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(500);

        GracefulShutdown shutdown = new GracefulShutdown(executor);
        shutdown.shutdown(2, TimeUnit.SECONDS);
    }

    /**
     * 辅助方法：打印线程池状态
     */
    private static void printThreadPoolStatus(ThreadPoolExecutor executor, String label) {
        System.out.println("\n[" + label + "]");
        System.out.println("  核心线程数: " + executor.getCorePoolSize());
        System.out.println("  最大线程数: " + executor.getMaximumPoolSize());
        System.out.println("  当前线程数: " + executor.getPoolSize());
        System.out.println("  活跃线程数: " + executor.getActiveCount());
        System.out.println("  队列大小: " + executor.getQueue().size());
        System.out.println("  已完成任务数: " + executor.getCompletedTaskCount());
        System.out.println("  总任务数: " + executor.getTaskCount());
        System.out.println("  是否关闭: " + executor.isShutdown());
        System.out.println("  是否终止: " + executor.isTerminated());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 最佳实践与注意事项
     */
    public static void bestPractices() {
        System.out.println("\n=== 最佳实践与注意事项 ===");

        System.out.println("""
        1. 监控关键指标
           - 活跃线程数
           - 队列大小
           - 已完成任务数
           - 拒绝任务数

        2. 设置合理告警阈值
           - 队列使用率 > 70%
           - 活跃线程率 > 80%
           - 拒绝任务数 > 0

        3. 动态调整线程池
           - 根据负载调整核心线程数
           - 根据资源情况调整最大线程数
           - 避免频繁调整

        4. CPU 密集型任务
           - 核心线程数 = CPU 核心数 + 1
           - 使用较小的队列
           - 减少线程切换

        5. IO 密集型任务
           - 核心线程数 = CPU 核心数 * 2
           - 可以使用较大的队列
           - 多等待一些线程

        6. 优雅关闭
           - 先调用 shutdown()
           - 使用 awaitTermination() 等待
           - 超时后 shutdownNow()

        7. 自定义拒绝策略
           - 记录拒绝的任务
           - 考虑降级方案
           - 发送告警通知

        8. 监控任务执行时间
           - 识别慢任务
           - 优化任务逻辑
           - 考虑任务超时

        9. 避免线程泄漏
           - 确保任务能正确结束
           - 正确处理中断
           - 清理资源

        10. 定期检查线程池状态
            - 实现健康检查
            - 定期输出状态日志
            - 设置监控告警
        """);
    }

    public static void main(String[] args) throws InterruptedException {
        basicMonitoring();
        customRejectionPolicy();
        continuousMonitoring();
        dynamicThreadPoolAdjustment();
        threadPoolAlerting();
        taskExecutionTimeStatistics();
        threadPoolHealthCheck();
        gracefulShutdown();
        bestPractices();
    }
}
