package com.example.juc;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * 虚拟线程学习示例
 * Java 21 (Project Loom) 引入的轻量级线程
 *
 * 运行要求: Java 21+
 * 编译: javac --enable-preview --release 21 VirtualThreadExample.java
 * 运行: java --enable-preview VirtualThreadExample
 *
 * Maven 配置需要在 pom.xml 中添加:
 * <plugin>
 *   <groupId>org.apache.maven.plugins</groupId>
 *   <artifactId>maven-compiler-plugin</artifactId>
 *   <version>3.13.0</version>
 *   <configuration>
 *     <source>21</source>
 *     <target>21</target>
 *     <compilerArgs>--enable-preview</compilerArgs>
 *   </configuration>
 * </plugin>
 */
public class VirtualThreadExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 虚拟线程学习示例 (Java 21+) ===\n");

        // 检查是否支持虚拟线程
        if (!supportsVirtualThreads()) {
            System.out.println("警告: 当前 JDK 版本不支持虚拟线程");
            System.out.println("请使用 JDK 21+ 并启用 preview 特性");
            return;
        }

        // 1. 创建虚拟线程的方式
        System.out.println("1. 创建虚拟线程的方式:");
        createVirtualThreads();

        // 2. 虚拟线程 vs 平台线程对比
        System.out.println("\n2. 虚拟线程 vs 平台线程对比:");
        virtualVsPlatformThreads();

        // 3. 虚拟线程中的阻塞操作
        System.out.println("\n3. 虚拟线程中的阻塞操作:");
        blockingOperations();

        // 4. 使用 ExecutorService 创建虚拟线程
        System.out.println("\n4. 使用 ExecutorService 批量创建虚拟线程:");
        virtualThreadExecutor();

        // 5. 结构化并发预览特性
        System.out.println("\n5. 结构化并发 (StructuredTaskScope):");
        if (supportsStructuredConcurrency()) {
            structuredConcurrency();
        } else {
            System.out.println("  当前版本不支持结构化并发");
        }

        // 6. 虚拟线程与同步
        System.out.println("\n6. 虚拟线程中的同步:");
        synchronizationInVirtual();

        // 7. 虚拟线程注意事项
        System.out.println("\n7. 虚拟线程注意事项:");
        virtualThreadPitfalls();
    }

    // 检查是否支持虚拟线程
    private static boolean supportsVirtualThreads() {
        try {
            Class.forName("java.lang.VirtualThread");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // 检查是否支持结构化并发
    private static boolean supportsStructuredConcurrency() {
        try {
            Class.forName("java.util.concurrent.StructuredTaskScope");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // 1. 创建虚拟线程的方式
    private static void createVirtualThreads() throws InterruptedException {
        // 方式1: Thread.ofVirtual() 创建并启动
        Thread v1 = Thread.ofVirtual().start(() -> {
            System.out.println("  方式1: 虚拟线程 - " + Thread.currentThread().getName());
        });
        v1.join();

        // 方式2: Thread.ofVirtual() 先创建后启动
        Thread v2 = Thread.ofVirtual().name("NamedVirtualThread").unstarted(() -> {
            System.out.println("  方式2: 命名虚拟线程 - " + Thread.currentThread().getName());
        });
        v2.start();
        v2.join();

        // 方式3: Thread.startVirtualThread() 简化方式
        Thread.startVirtualThread(() -> {
            System.out.println("  方式3: startVirtualThread 简化方式");
        }).join();

        // 方式4: 通过 ExecutorService
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> {
                System.out.println("  方式4: ExecutorService 创建虚拟线程");
            }).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. 虚拟线程 vs 平台线程对比
    private static void virtualVsPlatformThreads() throws InterruptedException {
        final int THREAD_COUNT = 1000;
        final int SLEEP_MS = 100;

        // 平台线程
        long platformStart = System.currentTimeMillis();
        CountDownLatch platformLatch = new CountDownLatch(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int taskId = i;
            new Thread(() -> {
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    platformLatch.countDown();
                }
            }, "Platform-" + taskId).start();
        }
        platformLatch.await();
        long platformTime = System.currentTimeMillis() - platformStart;

        System.out.println("  平台线程创建 " + THREAD_COUNT + " 个线程，耗时: " + platformTime + "ms");

        // 虚拟线程
        long virtualStart = System.currentTimeMillis();
        CountDownLatch virtualLatch = new CountDownLatch(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int taskId = i;
            Thread.startVirtualThread(() -> {
                try {
                    Thread.sleep(SLEEP_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    virtualLatch.countDown();
                }
            });
        }
        virtualLatch.await();
        long virtualTime = System.currentTimeMillis() - virtualStart;

        System.out.println("  虚拟线程创建 " + THREAD_COUNT + " 个线程，耗时: " + virtualTime + "ms");
        System.out.println("  性能提升: " + ((double) platformTime / virtualTime) + "x");

        // 虚拟线程栈大小对比
        System.out.println("\n  栈内存占用对比:");
        Thread platformThread = new Thread(() -> {
            System.out.println("    平台线程栈大小: ~1MB (默认)");
        });
        Thread virtualThread = Thread.ofVirtual().unstarted(() -> {
            System.out.println("    虚拟线程栈大小: ~几KB (动态调整)");
        });
    }

    // 3. 虚拟线程中的阻塞操作
    private static void blockingOperations() throws InterruptedException {
        System.out.println("  虚拟线程在 I/O 阻塞时不会占用平台线程");

        CountDownLatch latch = new CountDownLatch(2);

        // 模拟 I/O 阻塞
        Thread.startVirtualThread(() -> {
            try {
                System.out.println("  模拟 I/O 操作开始...");
                Thread.sleep(1000);
                System.out.println("  模拟 I/O 操作完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        // 模拟网络请求
        Thread.startVirtualThread(() -> {
            try {
                System.out.println("  模拟网络请求...");
                Thread.sleep(800);
                System.out.println("  网络请求完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        System.out.println("  所有虚拟任务完成");
    }

    // 4. 使用 ExecutorService 批量创建虚拟线程
    private static void virtualThreadExecutor() throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            System.out.println("  批量执行 10 个虚拟任务:");

            var futures = IntStream.range(0, 10)
                .mapToObj(i -> executor.submit(() -> {
                    try {
                        Thread.sleep(500);
                        return "任务-" + i + " 完成";
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return "任务-" + i + " 被中断";
                    }
                }))
                .toList();

            for (var future : futures) {
                System.out.println("    " + future.get());
            }
        }
        System.out.println("  ExecutorService 自动关闭");
    }

    // 5. 结构化并发 (预览特性)
    private static void structuredConcurrency() throws Exception {
        System.out.println("  结构化并发示例:");

        try {
            Class<?> scopeClass = Class.forName("java.util.concurrent.StructuredTaskScope");
            Object scope = scopeClass.getDeclaredConstructor().newInstance();

            // 创建子任务
            Object task1 = scopeClass.getMethod("fork", Callable.class)
                .invoke(scope, (Callable<String>) () -> {
                    Thread.sleep(500);
                    return "用户信息";
                });

            Object task2 = scopeClass.getMethod("fork", Callable.class)
                .invoke(scope, (Callable<String>) () -> {
                    Thread.sleep(300);
                    return "订单信息";
                });

            // 等待所有任务完成
            scopeClass.getMethod("joinUntil", Instant.class)
                .invoke(scope, Instant.now().plus(Duration.ofSeconds(5)));

            System.out.println("    " + ((Future<?>) task1).get());
            System.out.println("    " + ((Future<?>) task2).get());

        } catch (Exception e) {
            System.out.println("    结构化并发执行异常: " + e.getMessage());
        }
    }

    // 6. 虚拟线程中的同步
    private static void synchronizationInVirtual() throws InterruptedException {
        System.out.println("  虚拟线程可以使用 synchronized 和 Lock");

        Counter counter = new Counter();
        int threadCount = 100;
        int incrementsPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        long start = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            Thread.startVirtualThread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.increment();
                }
                latch.countDown();
            });
        }

        latch.await();
        long time = System.currentTimeMillis() - start;

        System.out.println("  " + threadCount + " 个虚拟线程，每个递增 " + incrementsPerThread + " 次");
        System.out.println("  最终计数: " + counter.getCount());
        System.out.println("  期望值: " + (threadCount * incrementsPerThread));
        System.out.println("  耗时: " + time + "ms");
        System.out.println("  结论: synchronized 在虚拟线程中同样有效");
    }

    // 7. 虚拟线程注意事项
    private static void virtualThreadPitfalls() {
        System.out.println("  虚拟线程限制和注意事项:");
        System.out.println("    1. 不建议使用 synchronized 锁保护长时间操作");
        System.out.println("       - 虚拟线程在 synchronized 块中阻塞时会挂载平台线程");
        System.out.println("       - 建议使用 ReentrantLock 替代");
        System.out.println();
        System.out.println("    2. 线程局部变量 (ThreadLocal) 可能导致内存泄漏");
        System.out.println("       - 大量虚拟线程可能创建大量 ThreadLocal 数据");
        System.out.println("       - 考虑使用 ScopedValue (预览特性)");
        System.out.println();
        System.out.println("    3. 避免在虚拟线程中使用固定大小的线程池");
        System.out.println("       - 固定线程池会阻塞虚拟线程");
        System.out.println("       - 虚拟线程池应使用 newVirtualThreadPerTaskExecutor");
        System.out.println();
        System.out.println("    4. CPU 密集型任务不建议使用虚拟线程");
        System.out.println("       - 虚拟线程适合 I/O 密集型任务");
        System.out.println("       - CPU 密集型任务仍使用平台线程或 ForkJoinPool");
        System.out.println();
        System.out.println("    5. 虚拟线程不适合作为守护线程");
        System.out.println("       - 虚拟线程默认是守护线程");
        System.out.println("       - 不能设置为非守护线程");
    }

    // 简单的线程安全计数器
    static class Counter {
        private int count = 0;

        public void increment() {
            synchronized (this) {
                count++;
            }
        }

        public int getCount() {
            return count;
        }
    }
}
