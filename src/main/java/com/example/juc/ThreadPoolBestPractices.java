package com.example.juc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;

/**
 * 线程池最佳实践
 * 正确创建、使用和监控线程池
 */
public class ThreadPoolBestPractices {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 线程池最佳实践 ===\n");

        // 1. 为什么不用Executors
        System.out.println("1. 为什么不建议使用Executors:");
        whyNotExecutors();

        // 2. 正确创建线程池
        System.out.println("\n2. 正确创建线程池:");
        createThreadPoolCorrectly();

        // 3. 线程池大小设置
        System.out.println("\n3. 线程池大小设置:");
        threadPoolSizing();

        // 4. 自定义ThreadFactory
        System.out.println("\n4. 自定义ThreadFactory:");
        customThreadFactory();

        // 5. 自定义拒绝策略
        System.out.println("\n5. 自定义拒绝策略:");
        customRejectionPolicy();

        // 6. 优雅关闭线程池
        System.out.println("\n6. 优雅关闭线程池:");
        gracefulShutdown();

        // 7. 线程池监控
        System.out.println("\n7. 线程池监控:");
        threadPoolMonitoring();

        // 8. 常见陷阱
        System.out.println("\n8. 常见陷阱:");
        commonPitfalls();
    }

    // 1. 为什么不用Executors
    private static void whyNotExecutors() {
        System.out.println("  Executors.newFixedThreadPool(n):");
        System.out.println("    ✗ 使用无界LinkedBlockingQueue");
        System.out.println("    ✗ 任务过多导致OOM");
        System.out.println();
        System.out.println("  Executors.newCachedThreadPool():");
        System.out.println("    ✗ 最大线程数Integer.MAX_VALUE");
        System.out.println("    ✗ 创建过多线程导致OOM");
        System.out.println();
        System.out.println("  Executors.newSingleThreadExecutor():");
        System.out.println("    ✗ 使用无界LinkedBlockingQueue");
        System.out.println("    ✗ 任务堆积导致OOM");
        System.out.println();
        System.out.println("  建议: 使用ThreadPoolExecutor，明确设置参数");
    }

    // 2. 正确创建线程池
    private static void createThreadPoolCorrectly() {
        // 推荐方式
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            4,                      // 核心线程数
            10,                     // 最大线程数
            60L,                    // 空闲线程存活时间
            TimeUnit.SECONDS,        // 时间单位
            new LinkedBlockingQueue<>(100),  // 有界队列
            new NamedThreadFactory("Worker"), // 自定义线程工厂
            new LoggingRejectedPolicy()        // 自定义拒绝策略
        );

        System.out.println("  线程池配置:");
        System.out.println("    核心线程数: " + executor.getCorePoolSize());
        System.out.println("    最大线程数: " + executor.getMaximumPoolSize());
        System.out.println("    队列容量: 100");

        // 测试执行
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("    执行任务" + taskId);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
    }

    // 3. 线程池大小设置
    private static void threadPoolSizing() {
        System.out.println("  CPU密集型任务:");
        System.out.println("    线程数 = CPU核心数 + 1");
        System.out.println("    原因: 充分利用CPU，减少线程切换");
        System.out.println("    示例: 8核CPU -> 9个线程");
        System.out.println();
        System.out.println("  IO密集型任务:");
        System.out.println("    线程数 = CPU核心数 * (1 + IO等待时间/CPU计算时间)");
        System.out.println("    原因: IO等待时CPU可以处理其他线程");
        System.out.println("    示例: 8核CPU，IO/CPU=2 -> 24个线程");
        System.out.println();
        System.out.println("  混合型任务:");
        System.out.println("    使用不同线程池分离CPU密集型和IO密集型任务");
        System.out.println("    或使用动态调整的线程池");
    }

    // 4. 自定义ThreadFactory
    private static void customThreadFactory() throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10),
            new NamedThreadFactory("CustomWorker")
        );

        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("  " + Thread.currentThread().getName() + " 处理任务" + taskId);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(500);
        executor.shutdown();
    }

    // 命名线程工厂
    static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
            thread.setDaemon(false); // 设置为非守护线程
            thread.setPriority(Thread.NORM_PRIORITY); // 设置优先级
            return thread;
        }
    }

    // 5. 自定义拒绝策略
    private static void customRejectionPolicy() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1),
            new NamedThreadFactory("Test"),
            new LoggingRejectedPolicy()
        );

        System.out.println("  提交3个任务，但线程池容量=1+1=2:");

        // 提交任务
        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            try {
                executor.submit(() -> {
                    System.out.println("    任务" + taskId + " 开始执行");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println("    任务" + taskId + " 完成");
                });
                System.out.println("    任务" + taskId + " 已提交");
            } catch (RejectedExecutionException e) {
                System.out.println("    任务" + taskId + " 被拒绝");
            }
        }

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdown();
    }

    // 自定义拒绝策略
    static class LoggingRejectedPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            System.err.println("  [拒绝策略] 任务被拒绝");
            System.err.println("    当前活跃线程: " + executor.getActiveCount());
            System.err.println("    队列大小: " + executor.getQueue().size());
            System.err.println("    已完成任务: " + executor.getCompletedTaskCount());

            // 可以记录日志或进行其他处理
            // 不应该直接丢弃任务
        }
    }

    // 6. 优雅关闭线程池
    private static void gracefulShutdown() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10)
        );

        // 提交一些任务
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(500);
                    System.out.println("  任务" + taskId + " 完成");
                } catch (InterruptedException e) {
                    System.out.println("  任务" + taskId + " 被中断");
                    Thread.currentThread().interrupt();
                }
            });
        }

        System.out.println("  开始关闭线程池...");

        // 优雅关闭
        executor.shutdown(); // 停止接受新任务

        try {
            // 等待所有任务完成
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                // 超时后尝试强制关闭
                System.out.println("  部分任务未完成，尝试强制关闭...");
                List<Runnable> unfinished = executor.shutdownNow();
                System.out.println("  未完成的任务数: " + unfinished.size());
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("  线程池已关闭: " + executor.isTerminated());
    }

    // 7. 线程池监控
    private static void threadPoolMonitoring() throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(5)
        );

        // 启动监控线程
        Thread monitor = new Thread(() -> {
            while (!executor.isTerminated()) {
                System.out.println("  [监控] " + getPoolStatus(executor));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        monitor.start();

        // 提交任务
        for (int i = 1; i <= 8; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            Thread.sleep(200);
        }

        Thread.sleep(4000);
        executor.shutdown();
        monitor.join();
    }

    // 获取线程池状态
    private static String getPoolStatus(ThreadPoolExecutor executor) {
        return String.format(
            "核心:%d, 活跃:%d, 最大:%d, 队列:%d, 已完成:%d",
            executor.getCorePoolSize(),
            executor.getActiveCount(),
            executor.getMaximumPoolSize(),
            executor.getQueue().size(),
            executor.getCompletedTaskCount()
        );
    }

    // 8. 常见陷阱
    private static void commonPitfalls() {
        System.out.println("  陷阱1: 使用ThreadLocal后不清理");
        System.out.println("    - 使用完ThreadLocal后必须remove()");
        System.out.println("    - 线程池中线程会被复用");
        System.out.println("    - 可能导致内存泄漏");
        System.out.println();
        System.out.println("  陷阱2: 任务中抛出未捕获异常");
        System.out.println("    - 异常会被ThreadPoolExecutor吞掉");
        System.out.println("    - 需要设置UncaughtExceptionHandler");
        System.out.println("    - 或使用Future.get()捕获异常");
        System.out.println();
        System.out.println("  陷阱3: 在任务中使用阻塞操作且没有超时");
        System.out.println("    - 可能导致线程永久阻塞");
        System.out.println("    - 影响线程池性能");
        System.out.println("    - 应该设置超时");
        System.out.println();
        System.out.println("  陷阱4: 提交依赖关系的任务到同一个线程池");
        System.out.println("    - 可能导致死锁");
        System.out.println("    - 任务相互等待对方释放线程");
        System.out.println("    - 应该使用不同线程池");
        System.out.println();
        System.out.println("  陷阱5: 队列设置过大");
        System.out.println("    - 导致任务堆积");
        System.out.println("    - 响应时间变长");
        System.out.println("    - 可能触发OOM");
    }
}
