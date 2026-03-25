package com.example.juc;

import java.util.concurrent.*;
import java.util.Map;

/**
 * 并发集合学习示例
 * 演示各种线程安全的集合类
 */
public class ConcurrentCollectionsExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 并发集合学习示例 ===");

        // 1. ConcurrentHashMap示例
        System.out.println("\n1. ConcurrentHashMap示例:");
        ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();

        // 并发put操作
        Thread[] mapThreads = new Thread[3];
        for (int i = 0; i < mapThreads.length; i++) {
            final int threadId = i;
            mapThreads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    String key = "key-" + threadId + "-" + j;
                    concurrentMap.put(key, j);
                }
            });
        }

        for (Thread t : mapThreads) t.start();
        for (Thread t : mapThreads) t.join();

        System.out.println("ConcurrentHashMap大小: " + concurrentMap.size());
        System.out.println("包含键 'key-0-50': " + concurrentMap.containsKey("key-0-50"));

        // 2. CopyOnWriteArrayList示例
        System.out.println("\n2. CopyOnWriteArrayList示例:");
        CopyOnWriteArrayList<String> copyOnWriteList = new CopyOnWriteArrayList<>();

        // 添加初始元素
        for (int i = 0; i < 5; i++) {
            copyOnWriteList.add("item-" + i);
        }

        // 并发读写操作
        Thread reader = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                System.out.println("读操作 - 列表大小: " + copyOnWriteList.size());
                for (String item : copyOnWriteList) {
                    System.out.println("  读取: " + item);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Thread writer = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                copyOnWriteList.add("new-item-" + i);
                System.out.println("写操作 - 添加: new-item-" + i);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        reader.start();
        writer.start();
        reader.join();
        writer.join();

        // 3. BlockingQueue示例
        System.out.println("\n3. BlockingQueue示例:");
        BlockingQueue<String> blockingQueue = new LinkedBlockingQueue<>(3);

        // 生产者线程
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    String item = "product-" + i;
                    blockingQueue.put(item); // 阻塞直到有空间
                    System.out.println("生产: " + item);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 消费者线程
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    String item = blockingQueue.take(); // 阻塞直到有元素
                    System.out.println("消费: " + item);
                    Thread.sleep(800);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();

        // 4. ConcurrentLinkedQueue示例
        System.out.println("\n4. ConcurrentLinkedQueue示例:");
        ConcurrentLinkedQueue<String> concurrentQueue = new ConcurrentLinkedQueue<>();

        // 并发offer和poll操作
        Thread[] queueThreads = new Thread[4];
        for (int i = 0; i < 2; i++) {
            final int threadId = i;
            queueThreads[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    String item = "offer-item-" + threadId + "-" + j;
                    concurrentQueue.offer(item);
                    System.out.println("入队: " + item);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        for (int i = 2; i < 4; i++) {
            queueThreads[i] = new Thread(() -> {
                for (int j = 0; j < 5; j++) {
                    String item = concurrentQueue.poll();
                    if (item != null) {
                        System.out.println("出队: " + item);
                    }
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        for (Thread t : queueThreads) t.start();
        for (Thread t : queueThreads) t.join();

        System.out.println("队列剩余元素数量: " + concurrentQueue.size());
    }
}