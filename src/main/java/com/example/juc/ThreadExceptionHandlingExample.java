package com.example.juc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程异常处理示例
 * <p>
 * 演示线程中异常的处理方式，以及如何设置异常处理器
 * </p>
 *
 * <h3>线程异常传播：</h3>
 * <ul>
 *   <li>线程中的未捕获异常会导致线程终止</li>
 *   <li>异常不会传播到调用者（start() 方法的调用者）</li>
 *   <li>可以通过 UncaughtExceptionHandler 捕获异常</li>
 *   <li>ExecutorService 可以通过 Future 获取异常</li>
 * </ul>
 *
 * <h3>异常处理方式：</h3>
 * <ul>
 *   <li>UncaughtExceptionHandler - 全局/线程级异常处理器</li>
 *   <li>Future.get() - 获取任务执行异常</li>
 *   <li>ThreadGroup - 处理组内线程异常</li>
 * </ul>
 */
public class ThreadExceptionHandlingExample {

    /**
     * 场景1：未捕获异常导致线程终止
     * <p>
     * 演示线程中抛出未捕获异常时的情况
     * </p>
     */
    public static void uncaughtException() throws InterruptedException {
        System.out.println("=== 场景1：未捕获异常 ===");

        Thread t = new Thread(() -> {
            System.out.println("线程开始执行");
            System.out.println("准备抛出异常...");
            throw new RuntimeException("线程运行时异常");
        });

        t.start();
        t.join();

        System.out.println("主线程继续执行");
    }

    /**
     * 场景2：使用 UncaughtExceptionHandler
     * <p>
     * 设置线程的异常处理器来捕获未捕获的异常
     * </p>
     */
    public static void uncaughtExceptionHandler() throws InterruptedException {
        System.out.println("\n=== 场景2：UncaughtExceptionHandler ===");

        Thread.UncaughtExceptionHandler handler = (thread, throwable) -> {
            System.out.println("异常处理器被调用:");
            System.out.println("  线程名称: " + thread.getName());
            System.out.println("  异常类型: " + throwable.getClass().getName());
            System.out.println("  异常消息: " + throwable.getMessage());
        };

        Thread t = new Thread(() -> {
            System.out.println("线程开始执行");
            throw new RuntimeException("线程运行时异常");
        });

        t.setUncaughtExceptionHandler(handler);
        t.start();
        t.join();
    }

    /**
     * 场景3：默认 UncaughtExceptionHandler
     * <p>
     * 设置全局默认的异常处理器
     * </p>
     */
    public static void defaultUncaughtExceptionHandler() throws InterruptedException {
        System.out.println("\n=== 场景3：默认 UncaughtExceptionHandler ===");

        // 设置全局默认处理器
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.out.println("【默认异常处理器】线程 " + thread.getName() +
                    " 抛出异常: " + throwable.getMessage());
            // 可以记录日志、发送告警等
        });

        // 创建两个线程，不单独设置异常处理器
        Thread t1 = new Thread(() -> {
            throw new RuntimeException("线程1的异常");
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            throw new IllegalArgumentException("线程2的异常");
        }, "Thread-2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        // 清除默认处理器（可选）
        Thread.setDefaultUncaughtExceptionHandler(null);
    }

    /**
     * 场景4：ThreadGroup 处理异常
     * <p>
     * 使用 ThreadGroup 统一处理组内线程的异常
     * </p>
     */
    public static void threadGroupExceptionHandler() throws InterruptedException {
        System.out.println("\n=== 场景4：ThreadGroup 异常处理 ===");

        class ExceptionHandlingThreadGroup extends ThreadGroup {
            public ExceptionHandlingThreadGroup(String name) {
                super(name);
            }

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                System.out.println("【ThreadGroup】线程 " + t.getName() +
                        " (组: " + getName() + ") 抛出异常: " + e.getMessage());
            }
        }

        ThreadGroup group = new ExceptionHandlingThreadGroup("我的线程组");

        Thread t1 = new Thread(group, () -> {
            throw new RuntimeException("组内线程1的异常");
        }, "GroupThread-1");

        Thread t2 = new Thread(group, () -> {
            throw new RuntimeException("组内线程2的异常");
        }, "GroupThread-2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    /**
     * 场景5：ExecutorService 异常处理
     * <p>
     * 演示线程池中任务的异常处理方式
     * </p>
     */
    public static void executorServiceExceptionHandling() throws InterruptedException {
        System.out.println("\n=== 场景5：ExecutorService 异常处理 ===");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 使用 Callable 和 Future 处理异常
        Future<String> future1 = executor.submit(() -> {
            System.out.println("任务1开始");
            Thread.sleep(100);
            throw new RuntimeException("任务1抛出异常");
        });

        // 使用 Runnable，异常不会向上传播
        executor.submit(() -> {
            System.out.println("任务2开始");
            throw new RuntimeException("任务2抛出异常（不会抛出）");
        });

        Thread.sleep(500);

        try {
            String result = future1.get();
            System.out.println("任务1结果: " + result);
        } catch (ExecutionException e) {
            System.out.println("任务1执行异常: " + e.getCause().getMessage());
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    /**
     * 场景6：自定义 ThreadPoolExecutor 异常处理
     * <p>
     * 使用自定义的 ThreadFactory 设置每个线程的异常处理器
     * </p>
     */
    public static void customExecutorExceptionHandling() throws InterruptedException {
        System.out.println("\n=== 场景6：自定义 Executor 异常处理 ===");

        class ExceptionHandlingThreadFactory implements ThreadFactory {
            private final String namePrefix;
            private final AtomicInteger counter = new AtomicInteger(1);

            public ExceptionHandlingThreadFactory(String namePrefix) {
                this.namePrefix = namePrefix;
            }

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, namePrefix + "-" + counter.getAndIncrement());
                t.setUncaughtExceptionHandler((thread, throwable) -> {
                    System.out.println("【线程池异常】线程 " + thread.getName() +
                            " 异常: " + throwable.getMessage());
                    // 可以记录日志、发送告警等
                });
                return t;
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(
                2, new ExceptionHandlingThreadFactory("Worker"));

        executor.submit(() -> {
            throw new RuntimeException("线程池任务异常1");
        });

        executor.submit(() -> {
            throw new IllegalArgumentException("线程池任务异常2");
        });

        Thread.sleep(500);

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    /**
     * 场景7：CompletableFuture 异常处理
     * <p>
     * 演示 CompletableFuture 的异常处理方式
     * </p>
     */
    public static void completableFutureExceptionHandling() throws InterruptedException {
        System.out.println("\n=== 场景7：CompletableFuture 异常处理 ===");

        // exceptionally - 恢复异常
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务1执行");
            throw new RuntimeException("任务1异常");
        }).exceptionally(e -> {
            System.out.println("处理异常: " + e.getMessage());
            return "默认值";
        });

        System.out.println("结果1: " + future1.join());

        // handle - 无论成功或异常都处理
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务2执行");
            return "正常结果";
        }).handle((result, e) -> {
            if (e != null) {
                return "错误: " + e.getMessage();
            }
            return "成功: " + result;
        });

        System.out.println("结果2: " + future2.join());

        // whenComplete - 只用于副作用
        CompletableFuture.supplyAsync(() -> {
            System.out.println("任务3执行");
            throw new RuntimeException("任务3异常");
        }).whenComplete((result, e) -> {
            if (e != null) {
                System.out.println("任务3完成时发现异常: " + e.getMessage());
            } else {
                System.out.println("任务3成功完成");
            }
        }).join();
    }

    /**
     * 场景8：异常的传播链
     * <p>
     * 演示多层任务中的异常传播
     * </p>
     */
    public static void exceptionPropagationChain() throws InterruptedException {
        System.out.println("\n=== 场景8：异常传播链 ===");

        class TaskExecutor {
            private final ExecutorService executor = Executors.newFixedThreadPool(2);

            public void executeTask(int taskId) throws InterruptedException, ExecutionException {
                System.out.println("主任务 " + taskId + " 开始");
                try {
                    String result = executor.submit(() -> {
                        System.out.println("  子任务开始");
                        Thread.sleep(100);
                        return executor.submit(() -> {
                            System.out.println("    孙任务开始");
                            Thread.sleep(50);
                            throw new RuntimeException("最底层任务异常");
                        }).get();
                    }).get();
                    System.out.println("主任务 " + taskId + " 结果: " + result);
                } catch (ExecutionException e) {
                    System.out.println("主任务 " + taskId + " 捕获异常: " + e.getCause().getMessage());
                    throw e;
                }
            }

            public void shutdown() throws InterruptedException {
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.SECONDS);
            }
        }

        TaskExecutor executor = new TaskExecutor();

        try {
            executor.executeTask(1);
        } catch (ExecutionException e) {
            System.out.println("最终捕获的异常: " + e.getMessage());
        }

        executor.shutdown();
    }

    /**
     * 场景9：异常恢复和重试
     * <p>
     * 演示任务失败后的重试机制
     * </p>
     */
    public static void exceptionRecoveryAndRetry() throws InterruptedException {
        System.out.println("\n=== 场景9：异常恢复和重试 ===");

        class RetryExecutor {
            private final ExecutorService executor = Executors.newFixedThreadPool(2);

            public CompletableFuture<String> executeWithRetry(int maxRetries, Callable<String> task) {
                return CompletableFuture.supplyAsync(() -> {
                    int attempt = 0;
                    while (attempt <= maxRetries) {
                        try {
                            System.out.println("尝试 " + (attempt + 1) + "/" + (maxRetries + 1));
                            return task.call();
                        } catch (Exception e) {
                            attempt++;
                            if (attempt > maxRetries) {
                                System.out.println("所有重试失败");
                                throw new CompletionException(e);
                            }
                            System.out.println("失败，准备重试...");
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                throw new CompletionException(ie);
                            }
                        }
                    }
                    throw new CompletionException(new RuntimeException("未知错误"));
                }, executor);
            }

            public void shutdown() throws InterruptedException {
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.SECONDS);
            }
        }

        RetryExecutor retryExecutor = new RetryExecutor();
        AtomicInteger failCount = new AtomicInteger(0);

        // 前2次失败，第3次成功
        Callable<String> task = () -> {
            int count = failCount.incrementAndGet();
            if (count <= 2) {
                throw new RuntimeException("模拟失败");
            }
            return "成功";
        };

        try {
            String result = retryExecutor.executeWithRetry(2, task).get();
            System.out.println("最终结果: " + result);
        } catch (ExecutionException e) {
            System.out.println("任务最终失败: " + e.getCause().getMessage());
        }

        retryExecutor.shutdown();
    }

    /**
     * 最佳实践与注意事项
     */
    public static void bestPractices() {
        System.out.println("\n=== 最佳实践与注意事项 ===");

        System.out.println("""
        1. 始终设置 UncaughtExceptionHandler
           - 为每个线程设置异常处理器
           - 或设置全局默认处理器
           - 记录日志、发送告警

        2. ExecutorService 中使用 Future
           - 通过 Future.get() 获取异常
           - 使用 CompletionException 判断
           - 避免异常被吞没

        3. CompletableFuture 异常处理
           - exceptionally: 恢复异常，提供默认值
           - handle: 统一处理成功和失败
           - whenComplete: 用于副作用

        4. 自定义 ThreadFactory
           - 为线程池创建的线程设置异常处理器
           - 统一的命名和配置
           - 便于监控和追踪

        5. 异常恢复策略
           - 重试机制：自动重试失败的任务
           - 降级处理：提供备选方案
           - 快速失败：避免级联故障

        6. 记录详细日志
           - 记录线程名称、时间戳
           - 记录完整的堆栈跟踪
           - 记录上下文信息

        7. 监控和告警
           - 监控异常发生频率
           - 设置异常率告警
           - 及时发现问题

        8. ThreadGroup 使用
           - 适用于批量管理线程
           - 统一异常处理
           - 但现代应用中较少使用

        9. 避免异常泄漏
           - 捕获后适当处理
           - 不要简单地打印堆栈
           - 考虑异常的影响

        10. 优雅关闭
            - 确保资源被正确释放
            - 处理中断异常
            - 避免资源泄漏
        """);
    }

    public static void main(String[] args) throws InterruptedException {
        uncaughtException();
        uncaughtExceptionHandler();
        defaultUncaughtExceptionHandler();
        threadGroupExceptionHandler();
        executorServiceExceptionHandling();
        customExecutorExceptionHandling();
        completableFutureExceptionHandling();
        exceptionPropagationChain();
        exceptionRecoveryAndRetry();
        bestPractices();
    }
}
