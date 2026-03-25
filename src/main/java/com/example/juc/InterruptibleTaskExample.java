package com.example.juc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可中断任务处理示例
 * <p>
 * 演示如何正确处理线程中断，实现任务的优雅取消
 * </p>
 *
 * <h3>核心概念：</h3>
 * <ul>
 *   <li>中断标志位 - Thread.interrupted()</li>
 *   <li>可中断方法 - throws InterruptedException</li>
 *   <li>不可中断方法 - 需要定期检查中断标志</li>
 *   <li>InterruptedException - 被中断时抛出</li>
 * </ul>
 *
 * <h3>中断处理原则：</h3>
 * <ul>
 *   <li>捕获 InterruptedException 后，恢复中断状态</li>
 *   <li>定期检查中断标志</li>
 *   <li>正确清理资源</li>
   <li>快速响应中断</li>
 * </ul>
 */
public class InterruptibleTaskExample {

    /**
     * 场景1：基本的线程中断
     * <p>
     * 演示 interrupt()、isInterrupted() 和 interrupted() 的使用
     * </p>
     */
    public static void basicInterrupt() throws InterruptedException {
        System.out.println("=== 场景1：基本线程中断 ===");

        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("工作中...");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println("捕获到 InterruptedException");
                    System.out.println("中断标志: " + Thread.currentThread().isInterrupted());
                    // 恢复中断状态
                    Thread.currentThread().interrupt();
                    System.out.println("恢复中断标志后: " + Thread.currentThread().isInterrupted());
                    break;
                }
            }
            System.out.println("线程结束");
        });

        worker.start();
        Thread.sleep(2000);
        System.out.println("主线程中断 worker");
        worker.interrupt();
        worker.join();
    }

    /**
     * 场景2：正确处理 InterruptedException
     * <p>
     * 演示捕获异常后应该恢复中断状态
     * </p>
     */
    public static void properInterruptedExceptionHandling() throws InterruptedException {
        System.out.println("\n=== 场景2：正确处理 InterruptedException ===");

        // 错误的方式 - 没有恢复中断状态
        System.out.println("--- 错误方式 ---");
        Thread badThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("BadThread: 捕获异常但不恢复中断状态");
                // 没有调用 Thread.currentThread().interrupt()
            }
            // 这里的代码可能继续执行，这是不期望的
            System.out.println("BadThread: 继续执行（不应该）");
        });

        // 正确的方式 - 恢复中断状态
        System.out.println("\n--- 正确方式 ---");
        Thread goodThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("GoodThread: 捕获异常");
                Thread.currentThread().interrupt(); // 恢复中断状态
            }
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("GoodThread: 检测到中断，退出");
                return;
            }
            System.out.println("GoodThread: 继续执行");
        });

        badThread.start();
        Thread.sleep(100);
        badThread.interrupt();
        badThread.join();

        Thread.sleep(500);

        goodThread.start();
        Thread.sleep(100);
        goodThread.interrupt();
        goodThread.join();
    }

    /**
     * 场景3：不可中断任务的改造
     * <p>
     * 对于不可中断的阻塞操作，通过定期检查中断标志实现可中断
     * </p>
     */
    public static void makeNonInterruptibleTaskInterruptible() throws InterruptedException {
        System.out.println("\n=== 场景3：使不可中断任务可中断 ===");

        class LongTask {
            private final AtomicBoolean stopRequested = new AtomicBoolean(false);

            public void runBad() {
                // 不可中断 - 会一直运行直到完成
                for (long i = 0; i < Long.MAX_VALUE; i++) {
                    Math.sqrt(i);
                }
            }

            public void runGood() {
                // 可中断 - 定期检查中断标志
                for (long i = 0; i < Long.MAX_VALUE; i++) {
                    Math.sqrt(i);
                    // 定期检查中断
                    if (Thread.currentThread().isInterrupted() || stopRequested.get()) {
                        System.out.println("检测到中断请求，退出循环");
                        return;
                    }
                    // 每隔一定次数检查一次，减少开销
                    if (i % 100000000 == 0) {
                        Thread.yield();
                    }
                }
            }

            public void requestStop() {
                stopRequested.set(true);
            }
        }

        LongTask task = new LongTask();

        // 不可中断的任务
        Thread badWorker = new Thread(task::runBad, "BadWorker");
        badWorker.setDaemon(true); // 设置为守护线程，避免主线程无法退出
        badWorker.start();

        Thread.sleep(100);
        System.out.println("尝试中断不可中断任务...");
        badWorker.interrupt();
        Thread.sleep(200);
        System.out.println("不可中断任务仍在运行: " + badWorker.isAlive());

        // 可中断的任务
        Thread goodWorker = new Thread(task::runGood, "GoodWorker");
        goodWorker.start();

        Thread.sleep(200);
        System.out.println("\n中断可中断任务...");
        goodWorker.interrupt();
        goodWorker.join(1000);
        System.out.println("可中断任务状态: " + (goodWorker.isAlive() ? "仍在运行" : "已退出"));
    }

    /**
     * 场景4：在 ExecutorService 中处理中断
     * <p>
     * 演示提交到线程池的任务如何正确处理中断
     * </p>
     */
    public static void interruptInExecutorService() throws InterruptedException {
        System.out.println("\n=== 场景4：ExecutorService 中的中断处理 ===");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 提交一个长时间运行的任务
        Future<?> future = executor.submit(() -> {
            try {
                System.out.println("任务开始执行");
                for (int i = 0; i < 10; i++) {
                    // 定期检查中断
                    if (Thread.currentThread().isInterrupted()) {
                        System.out.println("检测到中断，清理资源后退出");
                        return;
                    }
                    System.out.println("步骤 " + (i + 1));
                    Thread.sleep(500);
                }
                System.out.println("任务正常完成");
            } catch (InterruptedException e) {
                System.out.println("捕获 InterruptedException");
                Thread.currentThread().interrupt(); // 恢复中断状态
                System.out.println("清理资源后退出");
            }
        });

        Thread.sleep(1200);
        System.out.println("取消任务");
        boolean cancelled = future.cancel(true); // true 表示可以中断正在运行的任务
        System.out.println("取消结果: " + cancelled);

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

    /**
     * 场景5：批量任务的中断处理
     * <p>
     * 演示批量提交任务时如何统一处理中断
     * </p>
     */
    public static void batchTaskInterruption() throws InterruptedException {
        System.out.println("\n=== 场景5：批量任务中断 ===");

        ExecutorService executor = Executors.newFixedThreadPool(4);
        AtomicInteger completedCount = new AtomicInteger(0);

        // 提交10个任务
        List<Future<?>> futures = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            Future<?> future = executor.submit(() -> {
                try {
                    System.out.println("任务 " + taskId + " 开始");
                    Thread.sleep(ThreadLocalRandom.current().nextInt(500, 2000));
                    System.out.println("任务 " + taskId + " 完成");
                    completedCount.incrementAndGet();
                } catch (InterruptedException e) {
                    System.out.println("任务 " + taskId + " 被中断");
                    Thread.currentThread().interrupt();
                }
            });
            futures.add(future);
        }

        Thread.sleep(1500);
        System.out.println("\n中断所有未完成的任务");
        for (Future<?> future : futures) {
            future.cancel(true);
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
        System.out.println("已完成任务数: " + completedCount.get());
    }

    /**
     * 场景6：可中断的 I/O 操作
     * <p>
     * 演示如何使 I/O 操作可中断
     * </p>
     */
    public static void interruptibleIO() throws InterruptedException {
        System.out.println("\n=== 场景6：可中断的 I/O 操作 ===");

        // 使用可中断的阻塞队列
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(1);

        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    String item = "Item-" + i;
                    System.out.println("生产: " + item);
                    queue.put(item); // 可中断
                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                System.out.println("生产者被中断");
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    String item = queue.take(); // 可中断
                    System.out.println("消费: " + item);
                    Thread.sleep(500);
                }
                // 中断生产者
                System.out.println("\n消费者中断生产者");
                producer.interrupt();
            } catch (InterruptedException e) {
                System.out.println("消费者被中断");
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
    }

    /**
     * 场景7：两阶段终止模式
     * <p>
     * 使用标志位和中断实现优雅的任务终止
     * </p>
     */
    public static void twoPhaseTermination() throws InterruptedException {
        System.out.println("\n=== 场景7：两阶段终止模式 ===");

        class TwoPhaseTerminationTask {
            private Thread worker;
            private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

            public void start() {
                worker = new Thread(() -> {
                    try {
                        while (!shutdownRequested.get()) {
                            System.out.println("执行任务...");
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        System.out.println("捕获到中断异常");
                        Thread.currentThread().interrupt();
                    } finally {
                        System.out.println("执行清理工作...");
                        Thread.sleep(200);
                        System.out.println("清理完成");
                    }
                    System.out.println("工作线程退出");
                });
                worker.start();
            }

            public void shutdown() {
                System.out.println("第一阶段：设置关闭标志");
                shutdownRequested.set(true);

                System.out.println("第二阶段：中断线程");
                worker.interrupt();
            }

            public void join() throws InterruptedException {
                worker.join();
            }
        }

        TwoPhaseTerminationTask task = new TwoPhaseTerminationTask();
        task.start();

        Thread.sleep(1500);
        task.shutdown();
        task.join();
    }

    /**
     * 场景8：使用 Future 的超时控制
     * <p>
     * 通过 get(timeout) 实现超时取消
     * </p>
     */
    public static void timeoutControlWithFuture() throws InterruptedException {
        System.out.println("\n=== 场景8：Future 超时控制 ===");

        ExecutorService executor = Executors.newSingleThreadExecutor();

        Callable<String> task = () -> {
            System.out.println("任务开始执行");
            Thread.sleep(3000);
            return "完成";
        };

        Future<String> future = executor.submit(task);

        try {
            System.out.println("等待结果（超时1秒）");
            String result = future.get(1, TimeUnit.SECONDS);
            System.out.println("获得结果: " + result);
        } catch (TimeoutException e) {
            System.out.println("任务超时，取消任务");
            future.cancel(true);
        } catch (ExecutionException e) {
            System.out.println("任务执行异常");
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

    /**
     * 最佳实践与注意事项
     */
    public static void bestPractices() {
        System.out.println("\n=== 最佳实践与注意事项 ===");

        System.out.println("""
        1. 捕获 InterruptedException 后的处理
           - 最好恢复中断状态：Thread.currentThread().interrupt()
           - 或者向上抛出（如果是方法签名允许）
           - 不要简单地忽略异常

        2. 定期检查中断标志
           - 在循环中检查 Thread.currentThread().isInterrupted()
           - 对于长时间运行的任务尤其重要
           - 检查频率根据性能需求调整

        3. 区分中断类型
           - isInterrupted() - 不清除中断标志
           - interrupted() - 清除中断标志
           - 根据需要选择使用

        4. 正确清理资源
           - 被（捕获）中断后应该清理已分配的资源
           - 使用 try-finally 确保资源释放

        5. 不可中断操作的处理
           - I/O、锁、Socket 等可能不可中断
           - 通过标志位实现可中断
           - 或使用可替代的可中断方法

        6. 使用 ExecutorService 时的中断
           - future.cancel(true) 可以中断运行中的任务
           - executor.shutdownNow() 会中断所有任务
           - 任务代码需要正确处理中断

        7. 超时控制
           - 使用 Future.get(timeout) 实现超时
           - 超时后可以取消任务
           - 避免无限等待

        8. 两阶段终止
           - 第一阶段：设置终止标志
           - 第二阶段：中断线程
           - 确保资源被正确清理

        9. 不要中断自己
           - Thread.currentThread().interrupt() 用于恢复状态
           - 不应该用于主动中断自己

        10. 中断是协作机制
            - 被中断的线程可以选择如何响应
            - 不是强制立即终止
            - 应该快速、优雅地响应中断
        """);
    }

    public static void main(String[] args) throws InterruptedException {
        basicInterrupt();
        properInterruptedExceptionHandling();
        makeNonInterruptibleTaskInterruptible();
        interruptInExecutorService();
        batchTaskInterruption();
        interruptibleIO();
        twoPhaseTermination();
        timeoutControlWithFuture();
        bestPractices();
    }
}
