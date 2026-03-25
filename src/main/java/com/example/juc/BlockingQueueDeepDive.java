package com.example.juc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BlockingQueue深度剖析
 * 学习生产者-消费者模式和阻塞队列的实现原理
 */
public class BlockingQueueDeepDive {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== BlockingQueue深度剖析 ===");

        // 1. 不同实现类的对比
        System.out.println("\n1. 不同实现类对比:");

        // ArrayBlockingQueue - 有界阻塞队列
        BlockingQueue<String> arrayQueue = new ArrayBlockingQueue<>(3);
        System.out.println("ArrayBlockingQueue - 基于数组的有界阻塞队列");

        // LinkedBlockingQueue - 链表阻塞队列（可选有界）
        BlockingQueue<String> linkedQueue = new LinkedBlockingQueue<>(3);
        System.out.println("LinkedBlockingQueue - 基于链表的阻塞队列");

        // PriorityBlockingQueue - 优先级阻塞队列
        BlockingQueue<Integer> priorityQueue = new PriorityBlockingQueue<>();
        System.out.println("PriorityBlockingQueue - 优先级阻塞队列");

        // SynchronousQueue - 同步队列（不存储元素）
        BlockingQueue<String> syncQueue = new SynchronousQueue<>();
        System.out.println("SynchronousQueue - 不存储元素的同步队列");

        // 2. 阻塞操作与非阻塞操作对比
        System.out.println("\n2. 阻塞与非阻塞操作对比:");
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);

        // 非阻塞操作
        System.out.println("非阻塞操作:");
        System.out.println("offer('item1'): " + queue.offer("item1")); // true
        System.out.println("offer('item2'): " + queue.offer("item2")); // true
        System.out.println("offer('item3'): " + queue.offer("item3")); // false (队列满)

        System.out.println("poll(): " + queue.poll()); // item1
        System.out.println("poll(): " + queue.poll()); // item2
        System.out.println("poll(): " + queue.poll()); // null (队列空)

        // 阻塞操作
        System.out.println("\n阻塞操作示例:");
        queue.offer("item1");
        queue.offer("item2");
        System.out.println("队列已满，准备添加第三个元素...");

        Thread producer = new Thread(() -> {
            try {
                Thread.sleep(2000); // 模拟处理时间
                queue.put("item3"); // 阻塞直到有空间
                System.out.println("成功添加item3");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(1000); // 1秒后消费一个元素
                String item = queue.take(); // 阻塞直到有元素
                System.out.println("消费了: " + item);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        // 3. 生产者-消费者模式完整示例
        System.out.println("\n3. 生产者-消费者模式:");
        BlockingQueue<Integer> workQueue = new LinkedBlockingQueue<>(5);
        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);

        // 生产者
        Thread producer1 = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    int item = produced.incrementAndGet();
                    workQueue.put(item);
                    System.out.println("生产者1 生产: " + item + ", 队列大小: " + workQueue.size());
                    Thread.sleep(100 + (int)(Math.random() * 200));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread producer2 = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    int item = produced.incrementAndGet();
                    workQueue.put(item);
                    System.out.println("生产者2 生产: " + item + ", 队列大小: " + workQueue.size());
                    Thread.sleep(150 + (int)(Math.random() * 200));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 消费者
        Thread consumer1 = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    Integer item = workQueue.take();
                    consumed.incrementAndGet();
                    System.out.println("消费者1 消费: " + item + ", 队列大小: " + workQueue.size());
                    Thread.sleep(200 + (int)(Math.random() * 300));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumer2 = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    Integer item = workQueue.take();
                    consumed.incrementAndGet();
                    System.out.println("消费者2 消费: " + item + ", 队列大小: " + workQueue.size());
                    Thread.sleep(180 + (int)(Math.random() * 250));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer1.start();
        producer2.start();
        consumer1.start();
        consumer2.start();

        producer1.join();
        producer2.join();
        consumer1.join();
        consumer2.join();

        System.out.println("生产总数: " + produced.get() + ", 消费总数: " + consumed.get());

        // 4. 超时操作
        System.out.println("\n4. 超时操作:");
        BlockingQueue<String> timeoutQueue = new ArrayBlockingQueue<>(2);

        // 带超时的offer
        boolean offered = timeoutQueue.offer("item1", 1, TimeUnit.SECONDS);
        System.out.println("offer with timeout: " + offered);

        // 带超时的poll
        try {
            String item = timeoutQueue.poll(2, TimeUnit.SECONDS);
            System.out.println("poll with timeout: " + item);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 5. 批量操作和drainTo
        System.out.println("\n5. 批量操作:");
        BlockingQueue<String> batchQueue = new LinkedBlockingQueue<>();

        // 添加一批元素
        for (int i = 0; i < 10; i++) {
            batchQueue.offer("batch-item-" + i);
        }
        System.out.println("队列大小: " + batchQueue.size());

        // drainTo - 批量转移到另一个集合
        java.util.List<String> drained = new java.util.ArrayList<>();
        int drainedCount = batchQueue.drainTo(drained, 5); // 最多转移5个
        System.out.println("drainTo转移了 " + drainedCount + " 个元素");
        System.out.println("剩余队列大小: " + batchQueue.size());
        System.out.println("转移的元素: " + drained);

        // 6. 不同BlockingQueue实现的特性
        System.out.println("\n6. 不同实现的特性:");

        // ArrayBlockingQueue - 公平性选择
        BlockingQueue<String> fairQueue = new ArrayBlockingQueue<>(5, true); // 公平锁
        BlockingQueue<String> unfairQueue = new ArrayBlockingQueue<>(5, false); // 非公平锁
        System.out.println("ArrayBlockingQueue支持公平性选择");

        // LinkedBlockingQueue - 可选边界
        BlockingQueue<String> boundedLinked = new LinkedBlockingQueue<>(10); // 有界
        BlockingQueue<String> unboundedLinked = new LinkedBlockingQueue<>(); // 无界
        System.out.println("LinkedBlockingQueue可配置为有界或无界");

        // DelayQueue - 延迟队列概念
        System.out.println("DelayQueue - 元素需要延迟到期才能取出");
        System.out.println("TransferQueue - 生产者可以等待消费者接收");
    }
}