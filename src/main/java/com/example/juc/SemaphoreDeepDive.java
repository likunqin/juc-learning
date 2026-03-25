package com.example.juc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Semaphore 深度剖析示例
 * <p>
 * Semaphore（信号量）是一个计数信号量，用于控制同时访问特定资源的线程数量。
 * </p>
 *
 * <h3>核心特性：</h3>
 * <ul>
 *   <li>维护一个许可计数器</li>
 *   <li>acquire() 获取许可，阻塞直到有可用许可</li>
 *   <li>release() 释放许可，增加可用许可数</li>
 *   <li>支持公平和非公平模式</li>
 * </ul>
 *
 * <h3>适用场景：</h3>
 * <ul>
 *   <li>限流：限制并发访问资源的数量</li>
 *   <li>资源池：管理有限资源的分配</li>
 *   <li>连接池：限制数据库连接数</li>
 *   <li>流量控制：API 接口限流</li>
 * </ul>
 */
public class SemaphoreDeepDive {

    /**
     * 场景1：API 接口限流
     * <p>
     * 限制同时访问 API 的请求数量
     * </p>
     */
    public static void apiRateLimit() throws InterruptedException {
        System.out.println("=== 场景1：API 接口限流 ===");

        // 最多允许 3 个并发请求
        Semaphore semaphore = new Semaphore(3);
        AtomicInteger requestCounter = new AtomicInteger(1);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        // 模拟 10 个请求
        for (int i = 0; i < 10; i++) {
            final int requestId = requestCounter.getAndIncrement();
            executor.submit(() -> {
                try {
                    System.out.println("请求 " + requestId + " 等待获取许可...");
                    semaphore.acquire();
                    System.out.println("请求 " + requestId + " 获取到许可，开始处理");

                    // 模拟请求处理
                    Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1500));
                    System.out.println("请求 " + requestId + " 处理完成");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    System.out.println("请求 " + requestId + " 释放许可");
                    semaphore.release();
                }
            });
        }

        Thread.sleep(8000);
        executor.shutdown();
    }

    /**
     * 场景2：数据库连接池模拟
     * <p>
     * 使用 Semaphore 管理有限的数据库连接
     * </p>
     */
    public static void connectionPool() throws InterruptedException {
        System.out.println("\n=== 场景2：数据库连接池 ===");

        int maxConnections = 3;
        Semaphore semaphore = new Semaphore(maxConnections);
        AtomicInteger activeConnections = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(8);

        // 模拟 8 个数据库操作
        for (int i = 0; i < 8; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("任务 " + taskId + " 请求连接...");
                    semaphore.acquire();

                    int current = activeConnections.incrementAndGet();
                    System.out.println("任务 " + taskId + " 获取连接，当前连接数: " + current);

                    // 模拟数据库操作
                    Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));

                    activeConnections.decrementAndGet();
                    System.out.println("任务 " + taskId + " 释放连接");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release();
                }
            });
        }

        Thread.sleep(10000);
        executor.shutdown();
    }

    /**
     * 场景3：公平 vs 非公平模式
     * <p>
     * 对比公平和非公平 Semaphore 的行为差异
     * </p>
     */
    public static void fairVsUnfair() throws InterruptedException {
        System.out.println("\n=== 场景3：公平 vs 非公平 ===");

        // 非公平模式（默认）
        System.out.println("--- 非公平模式 ---");
        testSemaphore(new Semaphore(2, false), "非公平");

        Thread.sleep(1000);

        // 公平模式
        System.out.println("\n--- 公平模式 ---");
        testSemaphore(new Semaphore(2, true), "公平");
    }

    private static void testSemaphore(Semaphore semaphore, String type) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        ExecutorService executor = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 5; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    // 先到的先阻塞，模拟等待顺序
                    if (id < 2) {
                        Thread.sleep(100);
                    }
                    System.out.println(type + " - 线程 " + id + " 尝试获取许可");
                    semaphore.acquire();
                    System.out.println(type + " - 线程 " + id + " 获取成功");
                    Thread.sleep(200);
                    semaphore.release();
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.await();
        executor.shutdown();
    }

    /**
     * 场景4：带超时的 acquire
     * <p>
     * 演示 tryAcquire 带超时的使用
     * </p>
     */
    public static void acquireWithTimeout() throws InterruptedException {
        System.out.println("\n=== 场景4：带超时的 acquire ===");

        Semaphore semaphore = new Semaphore(1);
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // 第一个线程长期占用许可
        executor.submit(() -> {
            try {
                semaphore.acquire();
                System.out.println("线程A 获取许可，将占用3秒");
                Thread.sleep(3000);
                System.out.println("线程A 释放许可");
                semaphore.release();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(200);

        // 其他线程尝试获取许可
        for (int i = 0; i < 2; i++) {
            final char threadName = (char) ('B' + i);
            executor.submit(() -> {
                try {
                    System.out.println("线程" + threadName + " 尝试获取许可（超时1秒）");
                    boolean acquired = semaphore.tryAcquire(1, TimeUnit.SECONDS);
                    if (acquired) {
                        System.out.println("线程" + threadName + " 获取成功");
                        semaphore.release();
                    } else {
                        System.out.println("线程" + threadName + " 获取超时，放弃等待");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(4000);
        executor.shutdown();
    }

    /**
     * 场景5：多许可 Semaphore
     * <p>
     * 一次获取/释放多个许可
     * </p>
     */
    public static void multiplePermits() throws InterruptedException {
        System.out.println("\n=== 场景5：多许可操作 ===");

        Semaphore semaphore = new Semaphore(5);

        System.out.println("初始许可数: " + semaphore.availablePermits());

        // 一次获取 2 个许可
        semaphore.acquire(2);
        System.out.println("获取2个许可后: " + semaphore.availablePermits());

        // 再获取 1 个许可
        semaphore.acquire();
        System.out.println("再获取1个许可后: " + semaphore.availablePermits());

        // 释放 3 个许可
        semaphore.release(3);
        System.out.println("释放3个许可后: " + semaphore.availablePermits());

        // 可以释放超过初始数量的许可
        semaphore.release(5);
        System.out.println("释放5个许可后（超过初始值）: " + semaphore.availablePermits());
    }

    /**
     * 场景6：信号量状态查询
     * <p>
     * 演示如何查询 Semaphore 的当前状态
     * </p>
     */
    public static void semaphoreStatus() throws InterruptedException {
        System.out.println("\n=== 场景6：信号量状态查询 ===");

        Semaphore semaphore = new Semaphore(3, true); // 公平模式
        System.out.println("初始状态:");
        printSemaphoreStatus(semaphore);

        CountDownLatch latch = new CountDownLatch(5);
        ExecutorService executor = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 5; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(200);
                    semaphore.acquire();
                    System.out.println("线程 " + id + " 获取许可，剩余: " +
                            semaphore.availablePermits() + ", 等待队列: " +
                            (5 - semaphore.availablePermits() - (semaphore.getQueueLength() > 0 ? 0 : 1)));
                    Thread.sleep(1000);
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        System.out.println("\n所有线程完成后状态:");
        printSemaphoreStatus(semaphore);

        executor.shutdown();
    }

    private static void printSemaphoreStatus(Semaphore semaphore) {
        System.out.println("  可用许可数: " + semaphore.availablePermits());
        System.out.println("  等待线程数: " + semaphore.getQueueLength());
        System.out.println("  是否公平: " + semaphore.isFair());
    }

    /**
     * 场景7：资源争抢（赛马场景）
     * <p>
     * 多个线程竞争有限资源
     * </p>
     */
    public static void resourceCompetition() throws InterruptedException {
        System.out.println("\n=== 场景7：资源争抢 ===");

        int resourceCount = 2;
        Semaphore semaphore = new Semaphore(resourceCount);
        AtomicInteger totalUsage = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(10);

        // 10 个工作线程竞争 2 个资源
        for (int i = 0; i < 10; i++) {
            final int workerId = i;
            executor.submit(() -> {
                try {
                    System.out.println("工人 " + workerId + " 等待资源...");
                    semaphore.acquire();

                    System.out.println("工人 " + workerId + " 获取资源，开始工作");
                    int usage = totalUsage.incrementAndGet();
                    Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1500));

                    System.out.println("工人 " + workerId + " 工作完成，释放资源");
                    semaphore.release();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(8000);
        System.out.println("资源总使用次数: " + totalUsage.get());
        executor.shutdown();
    }

    /**
     * 场景8：使用 tryAcquire() 实现非阻塞获取
     * <p>
     * 不阻塞，立即返回是否获取成功
     * </p>
     */
    public static void nonBlockingAcquire() throws InterruptedException {
        System.out.println("\n=== 场景8：非阻塞获取 ===");

        Semaphore semaphore = new Semaphore(2);

        // 第一个线程获取许可
        Thread t1 = new Thread(() -> {
            try {
                semaphore.acquire(2);
                System.out.println("线程A 占用了所有许可");
                Thread.sleep(2000);
                semaphore.release(2);
                System.out.println("线程A 释放许可");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t1.start();

        Thread.sleep(100);

        // 其他线程尝试非阻塞获取
        for (int i = 0; i < 3; i++) {
            final int id = i;
            Thread thread = new Thread(() -> {
                boolean acquired = semaphore.tryAcquire();
                if (acquired) {
                    System.out.println("线程 " + id + " 获取成功");
                    semaphore.release();
                } else {
                    System.out.println("线程 " + id + " 获取失败（无可用许可），执行其他操作...");
                }
            });
            thread.start();
        }

        Thread.sleep(3000);
    }

    /**
     * 最佳实践与注意事项
     */
    public static void bestPractices() {
        System.out.println("\n=== 最佳实践与注意事项 ===");

        System.out.println("""
        1. acquire() 必须在 finally 中调用 release()
           - 确保资源一定能被释放
           - 避免因异常导致许可泄漏

        2. 公平 vs 非公平的选择
           - 公平：先到先得，吞吐量较低
           - 非公平：允许插队，吞吐量较高（默认）
           - 根据业务场景选择

        3. 注意 tryAcquire 的返回值
           - 要检查返回值决定后续操作
           - 返回 false 时不能执行 release()

        4. release() 可以增加许可数
           - 允许超过初始许可数
           - 但不建议滥用，可能导致资源耗尽

        5. 使用 tryAcquire(timeout) 设置超时
           - 避免无限等待
           - 超时后可以执行降级逻辑

        6. 并发度控制
           - 根据资源能力设置合理的许可数
           - 不要设置过大，避免资源耗尽

        7. 监控等待队列
           - 使用 getQueueLength() 监控等待线程
           - 可以据此动态调整许可数
        """);
    }

    public static void main(String[] args) throws InterruptedException {
        apiRateLimit();
        connectionPool();
        fairVsUnfair();
        acquireWithTimeout();
        multiplePermits();
        semaphoreStatus();
        resourceCompetition();
        nonBlockingAcquire();
        bestPractices();
    }
}
