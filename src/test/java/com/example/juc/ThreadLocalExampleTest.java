package com.example.juc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ThreadLocal 单元测试
 */
class ThreadLocalExampleTest {

    @Test
    void testThreadLocalIsolation() throws InterruptedException {
        ThreadLocal<Integer> threadLocal = ThreadLocal.withInitial(() -> 0);
        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger resultSum = new AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    // 每个线程设置自己的值
                    threadLocal.set(threadId * 10);
                    Thread.sleep(100);

                    // 每个线程应该读取到自己的值
                    int value = threadLocal.get();
                    assertEquals(threadId * 10, value,
                        "线程 " + threadId + " 应该读取到自己的值");

                    resultSum.addAndGet(value);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    threadLocal.remove();
                    latch.countDown();
                }
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(30, resultSum.get(), "每个线程的值应该被正确累加");
    }

    @Test
    void testThreadLocalWithInitial() {
        ThreadLocal<Integer> threadLocal = ThreadLocal.withInitial(() -> 100);

        assertEquals(100, threadLocal.get(), "初始值应该是 100");

        threadLocal.set(200);
        assertEquals(200, threadLocal.get(), "设置后应该返回新值");

        threadLocal.remove();
        assertEquals(100, threadLocal.get(), "remove 后应该返回初始值");
    }

    @Test
    void testInheritableThreadLocal() throws InterruptedException {
        InheritableThreadLocal<Integer> inheritableThreadLocal = new InheritableThreadLocal<>();
        inheritableThreadLocal.set(100);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger childValue = new AtomicInteger();

        // 子线程应该继承父线程的值
        Thread childThread = new Thread(() -> {
            childValue.set(inheritableThreadLocal.get());
            latch.countDown();
        });

        childThread.start();
        latch.await(5, TimeUnit.SECONDS);

        assertEquals(100, childValue.get(), "子线程应该继承父线程的值");
    }

    @Test
    void testThreadLocalMemoryLeakPrevention() throws InterruptedException {
        ThreadLocal<byte[]> largeThreadLocal = new ThreadLocal<>();
        CountDownLatch latch = new CountDownLatch(100);
        AtomicInteger completedCount = new AtomicInteger(0);

        // 模拟大量线程使用 ThreadLocal
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                try {
                    // 分配较大内存
                    largeThreadLocal.set(new byte[1024 * 1024]); // 1MB
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // 正确清理 ThreadLocal
                    largeThreadLocal.remove();
                    completedCount.incrementAndGet();
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(100, completedCount.get(), "所有任务应该完成");
    }

    @Test
    void testMultipleThreadLocalInstances() throws InterruptedException {
        ThreadLocal<String> nameThreadLocal = ThreadLocal.withInitial(() -> "default");
        ThreadLocal<Integer> countThreadLocal = ThreadLocal.withInitial(() -> 0);

        CountDownLatch latch = new CountDownLatch(1);
        int[] results = new int[2];

        Thread thread = new Thread(() -> {
            try {
                nameThreadLocal.set("test-thread");
                countThreadLocal.set(42);

                results[0] = nameThreadLocal.get().equals("test-thread") ? 1 : 0;
                results[1] = countThreadLocal.get();
            } finally {
                nameThreadLocal.remove();
                countThreadLocal.remove();
                latch.countDown();
            }
        });

        thread.start();
        latch.await(5, TimeUnit.SECONDS);

        assertEquals(1, results[0], "ThreadLocal 名称应该正确");
        assertEquals(42, results[1], "ThreadLocal 计数应该正确");
    }
}
