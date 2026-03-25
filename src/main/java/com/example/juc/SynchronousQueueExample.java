package com.example.juc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SynchronousQueue学习示例
 * 同步队列 - 不存储元素，每个put必须等待一个take
 */
public class SynchronousQueueExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== SynchronousQueue学习示例 ===\n");

        // 1. 基本使用
        System.out.println("1. 基本使用 - 直接交接:");
        basicUsage();

        // 2. 实际场景 - 任务传递
        System.out.println("\n2. 实际场景 - 任务直接传递给工作线程:");
        taskHandoff();

        // 3. 公平 vs 非公平
        System.out.println("\n3. 公平模式 vs 非公平模式:");
        fairVsUnfair();

        // 4. 与其他队列对比
        System.out.println("\n4. SynchronousQueue vs 其他队列:");
        comparison();
    }

    // 1. 基本使用
    private static void basicUsage() throws InterruptedException {
        SynchronousQueue<String> queue = new SynchronousQueue<>();

        // 生产者线程
        Thread producer = new Thread(() -> {
            try {
                System.out.println("  生产者准备提交数据...");
                // put会阻塞，直到有消费者接收
                queue.put("重要数据");
                System.out.println("  生产者数据已被接收");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 消费者线程
        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(500); // 延迟消费
                System.out.println("  消费者准备接收数据...");
                String data = queue.take(); // 阻塞直到有数据
                System.out.println("  消费者收到: " + data);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }

    // 2. 实际场景 - 任务传递
    private static void taskHandoff() throws InterruptedException {
        SynchronousQueue<Runnable> taskQueue = new SynchronousQueue<>();
        AtomicInteger taskCount = new AtomicInteger(0);

        // 工作线程池
        int workerCount = 3;
        Thread[] workers = new Thread[workerCount];

        for (int i = 0; i < workerCount; i++) {
            final int workerId = i + 1;
            workers[i] = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // take会阻塞，等待任务
                        Runnable task = taskQueue.take();
                        System.out.println("  工人" + workerId + " 接收任务");
                        task.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "Worker-" + workerId);
        }

        // 启动工作线程
        for (Thread worker : workers) {
            worker.start();
        }

        // 生产者线程 - 提交任务
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                final int taskId = i;
                Runnable task = () -> {
                    try {
                        System.out.println("    执行任务" + taskId);
                        Thread.sleep(300);
                        System.out.println("    任务" + taskId + " 完成");
                        taskCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                };

                System.out.println("  提交任务" + taskId);
                try {
                    taskQueue.put(task); // 直接传递给工人
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        producer.start();
        producer.join();

        Thread.sleep(500);

        // 停止工作线程
        for (Thread worker : workers) {
            worker.interrupt();
        }
        for (Thread worker : workers) {
            worker.join();
        }

        System.out.println("  完成任务数: " + taskCount.get());
    }

    // 3. 公平 vs 非公平
    private static void fairVsUnfair() throws InterruptedException {
        System.out.println("  非公平模式（默认）:");
        runSynchronousQueue(false);

        System.out.println("\n  公平模式:");
        runSynchronousQueue(true);
    }

    private static void runSynchronousQueue(boolean fair) throws InterruptedException {
        SynchronousQueue<Integer> queue = new SynchronousQueue<>(fair);

        // 消费者先等待
        Thread[] consumers = new Thread[3];
        for (int i = 0; i < consumers.length; i++) {
            final int consumerId = i;
            consumers[i] = new Thread(() -> {
                try {
                    Integer item = queue.take();
                    System.out.println("    消费者" + consumerId + " 收到: " + item);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        for (Thread c : consumers) c.start();
        Thread.sleep(100);

        // 生产者提交数据
        Thread[] producers = new Thread[3];
        for (int i = 0; i < producers.length; i++) {
            final int item = i + 1;
            producers[i] = new Thread(() -> {
                try {
                    queue.put(item);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        for (Thread p : producers) p.start();

        for (Thread c : consumers) c.join();
        for (Thread p : producers) p.join();
    }


    // 4. 与其他队列对比
    private static void comparison() {
        System.out.println("  SynchronousQueue:");
        System.out.println("    ✓ 不存储元素，capacity=0");
        System.out.println("    ✓ 每个put必须等待take");
        System.out.println("    ✓ 直接在线程间传递数据");
        System.out.println("    ✓ 适用于任务传递、手递手模式");
        System.out.println("    ✓ 性能高（无缓冲）");
        System.out.println();
        System.out.println("  ArrayBlockingQueue:");
        System.out.println("    ✓ 有界缓冲区");
        System.out.println("    ✓ 生产者可批量put");
        System.out.println("    ✓ 消费者可批量take");
        System.out.println();
        System.out.println("  LinkedBlockingQueue:");
        System.out.println("    ✓ 可选有界/无界");
        System.out.println("    ✓ 基于链表");
        System.out.println("    ✓ 通用生产者-消费者场景");
        System.out.println();
        System.out.println("  TransferQueue:");
        System.out.println("    ✓ 生产者可选择是否等待");
        System.out.println("    ✓ transfer()等待消费者");
        System.out.println("    ✓ tryTransfer()不等待");
        System.out.println();
        System.out.println("  选择建议:");
        System.out.println("    - 直接传递、零延迟: SynchronousQueue");
        System.out.println("    - 有缓冲的传递: LinkedBlockingQueue");
        System.out.println("    - 可选等待的传递: TransferQueue");
        System.out.println("    - 公平性重要: ArrayBlockingQueue(fair=true)");
    }
}
