package com.example.juc;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic类学习示例
 * 演示原子操作类的使用
 */
public class AtomicExample {

    // AtomicInteger示例
    private static AtomicInteger counter = new AtomicInteger(0);

    // AtomicLong示例
    private static AtomicLong sequence = new AtomicLong(0);

    // AtomicReference示例
    private static AtomicReference<String> currentUser = new AtomicReference<>("None");

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Atomic类学习示例 ===");

        // 创建多个线程测试原子操作
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    // 原子递增
                    int newValue = counter.incrementAndGet();
                    long seqValue = sequence.incrementAndGet();

                    // 原子更新引用
                    currentUser.compareAndSet("None", "User-" + Thread.currentThread().getId());

                    if (j % 200 == 0) {
                        System.out.println(Thread.currentThread().getName() +
                                " - Counter: " + newValue +
                                ", Sequence: " + seqValue +
                                ", User: " + currentUser.get());
                    }
                }
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("最终结果:");
        System.out.println("Counter: " + counter.get());
        System.out.println("Sequence: " + sequence.get());
        System.out.println("Current User: " + currentUser.get());
    }
}