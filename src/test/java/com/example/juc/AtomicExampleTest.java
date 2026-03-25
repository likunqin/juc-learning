package com.example.juc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Atomic 类单元测试
 */
class AtomicExampleTest {

    @Test
    void testAtomicIntegerIncrement() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        int threads = 10;
        int incrementsPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threads);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.incrementAndGet();
                }
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threads * incrementsPerThread, counter.get(),
            "Atomic 应该保证线程安全的递增操作");
    }

    @Test
    void testAtomicIntegerCompareAndSet() {
        AtomicInteger atomic = new AtomicInteger(0);

        // 第一次 CAS 应该成功
        assertTrue(atomic.compareAndSet(0, 10), "期望值为 0 时 CAS 应该成功");
        assertEquals(10, atomic.get(), "更新后值应为 10");

        // 第二次 CAS 应该失败
        assertFalse(atomic.compareAndSet(0, 20), "期望值不匹配时 CAS 应该失败");
        assertEquals(10, atomic.get(), "CAS 失败后值不应改变");
    }

    @Test
    void testAtomicIntegerGetAndUpdate() {
        AtomicInteger atomic = new AtomicInteger(5);

        int oldValue = atomic.getAndUpdate(x -> x * 2);
        assertEquals(5, oldValue, "getAndUpdate 应该返回旧值");
        assertEquals(10, atomic.get(), "更新后值应为 10");
    }

    @Test
    void testAtomicIntegerUpdateAndGet() {
        AtomicInteger atomic = new AtomicInteger(5);

        int newValue = atomic.updateAndGet(x -> x * 2);
        assertEquals(10, newValue, "updateAndGet 应该返回新值");
        assertEquals(10, atomic.get(), "更新后值应为 10");
    }
}
