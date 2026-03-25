package com.example.juc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 线程池单元测试
 */
class ThreadPoolExampleTest {

    @Test
    void testFixedThreadPool() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(5, counter.get(), "所有任务都应该被执行");

        executor.shutdown();
    }

    @Test
    void testCachedThreadPool() throws InterruptedException {
        ExecutorService executor = Executors.newCachedThreadPool();
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                counter.incrementAndGet();
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(10, counter.get(), "所有任务都应该被执行");

        executor.shutdown();
    }

    @Test
    void testThreadPoolExecutor() throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,                          // 核心线程数
                4,                          // 最大线程数
                60, TimeUnit.SECONDS,       // 空闲线程存活时间
                new ArrayBlockingQueue<>(2)  // 任务队列
        );

        AtomicInteger completed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(6);

        for (int i = 0; i < 6; i++) {
            final int taskId = i;
            executor.submit(() -> {
                completed.incrementAndGet();
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(6, completed.get(), "所有任务都应该被执行");

        executor.shutdown();
    }

    @Test
    void testThreadPoolSubmit() throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<Integer> future = executor.submit(() -> {
            Thread.sleep(100);
            return 42;
        });

        Integer result = future.get();
        assertEquals(42, result, "Future 应该返回正确结果");

        executor.shutdown();
    }

    @Test
    void testThreadPoolInvokeAll() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        Callable<Integer> task1 = () -> { Thread.sleep(100); return 1; };
        Callable<Integer> task2 = () -> { Thread.sleep(200); return 2; };
        Callable<Integer> task3 = () -> { Thread.sleep(50); return 3; };

        long start = System.currentTimeMillis();
        java.util.List<Future<Integer>> futures = executor.invokeAll(
                java.util.Arrays.asList(task1, task2, task3));
        long elapsed = System.currentTimeMillis() - start;

        // 并行执行，应该接近最慢任务的时间
        assertTrue(elapsed >= 150 && elapsed < 300,
                "invokeAll 应该并行执行任务");

        assertEquals(3, futures.size(), "应该返回所有 Future");
        assertEquals(1, futures.get(0).get());
        assertEquals(2, futures.get(1).get());
        assertEquals(3, futures.get(2).get());

        executor.shutdown();
    }

    @Test
    void testThreadPoolInvokeAny() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        Callable<Integer> task1 = () -> { Thread.sleep(500); return 1; };
        Callable<Integer> task2 = () -> { Thread.sleep(100); return 2; };
        Callable<Integer> task3 = () -> { Thread.sleep(300); return 3; };

        long start = System.currentTimeMillis();
        Integer result = executor.invokeAny(java.util.Arrays.asList(task1, task2, task3));
        long elapsed = System.currentTimeMillis() - start;

        // 应该返回最快完成的结果
        assertEquals(2, result, "应该返回最快完成的结果");
        assertTrue(elapsed >= 100 && elapsed < 200,
                "invokeAny 应该返回最快完成的结果");

        executor.shutdown();
    }

    @Test
    void testThreadPoolRejectedExecution() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1,
                0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1)
        );

        AtomicInteger rejectedCount = new AtomicInteger(0);

        // 设置拒绝策略
        executor.setRejectedExecutionHandler((r, e) -> {
            rejectedCount.incrementAndGet();
        });

        // 核心线程执行一个任务
        executor.submit(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 队列中一个任务
        executor.submit(() -> {});

        // 第三个任务应该被拒绝
        executor.submit(() -> {});

        assertEquals(1, rejectedCount.get(), "应该有一个任务被拒绝");

        executor.shutdown();
    }

    @Test
    void testThreadPoolShutdown() throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 2,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );

        AtomicInteger completed = new AtomicInteger(0);

        // 提交一些任务
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(100);
                    completed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 优雅关闭
        executor.shutdown();

        // 等待关闭
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS),
                "线程池应该在合理时间内关闭");

        assertTrue(executor.isShutdown(), "线程池应该已关闭");
        assertTrue(executor.isTerminated(), "所有任务应该已完成");
        assertEquals(5, completed.get(), "所有任务应该已完成");
    }
}
