package com.example.juc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 同步工具类学习示例
 * 演示各种同步机制的使用
 */
public class SynchronizationExample {

    // ReentrantLock示例
    private static ReentrantLock lock = new ReentrantLock();
    private static int sharedCounter = 0;

    // ReentrantReadWriteLock示例
    private static ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private static String sharedData = "初始数据";

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 同步工具类学习示例 ===");

        // 1. ReentrantLock示例
        System.out.println("\n1. ReentrantLock示例:");
        Thread[] lockThreads = new Thread[3];
        for (int i = 0; i < lockThreads.length; i++) {
            lockThreads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    lock.lock();
                    try {
                        sharedCounter++;
                    } finally {
                        lock.unlock();
                    }
                }
            });
        }

        for (Thread t : lockThreads) t.start();
        for (Thread t : lockThreads) t.join();

        System.out.println("ReentrantLock保护后的counter值: " + sharedCounter);

        // 2. CountDownLatch示例
        System.out.println("\n2. CountDownLatch示例:");
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            new Thread(() -> {
                try {
                    System.out.println("任务 " + taskId + " 开始执行");
                    Thread.sleep(1000 + taskId * 500);
                    System.out.println("任务 " + taskId + " 执行完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(); // 等待所有任务完成
        System.out.println("所有任务已完成，主线程继续执行");

        // 3. CyclicBarrier示例
        System.out.println("\n3. CyclicBarrier示例:");
        CyclicBarrier barrier = new CyclicBarrier(3, () -> {
            System.out.println("所有线程到达屏障点，开始下一阶段");
        });

        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    System.out.println("线程 " + threadId + " 第一阶段完成");
                    barrier.await();
                    System.out.println("线程 " + threadId + " 第二阶段开始");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        Thread.sleep(3000);

        // 4. Semaphore示例
        System.out.println("\n4. Semaphore示例:");
        Semaphore semaphore = new Semaphore(2); // 最多2个线程同时访问

        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    semaphore.acquire();
                    System.out.println("线程 " + threadId + " 获取到许可");
                    Thread.sleep(2000);
                    System.out.println("线程 " + threadId + " 释放许可");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release();
                }
            }).start();
        }

        Thread.sleep(6000);

        // 5. ReadWriteLock示例
        System.out.println("\n5. ReadWriteLock示例:");
        Thread[] rwThreads = new Thread[4];

        // 读线程
        for (int i = 0; i < 2; i++) {
            rwThreads[i] = new Thread(() -> {
                rwLock.readLock().lock();
                try {
                    System.out.println("读线程读取数据: " + sharedData);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    rwLock.readLock().unlock();
                }
            });
        }

        // 写线程
        for (int i = 2; i < 4; i++) {
            final String newData = "更新后的数据" + i;
            rwThreads[i] = new Thread(() -> {
                rwLock.writeLock().lock();
                try {
                    System.out.println("写线程更新数据为: " + newData);
                    sharedData = newData;
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    rwLock.writeLock().unlock();
                }
            });
        }

        for (Thread t : rwThreads) t.start();
        for (Thread t : rwThreads) t.join();

        System.out.println("最终数据: " + sharedData);
    }
}