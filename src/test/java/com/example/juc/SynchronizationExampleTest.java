package com.example.juc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 同步工具类单元测试
 */
class SynchronizationExampleTest {

    @Test
    void testCountDownLatch() throws InterruptedException {
        int parties = 5;
        CountDownLatch latch = new CountDownLatch(parties);
        AtomicInteger counter = new AtomicInteger(0);

        for (int i = 0; i < parties; i++) {
            new Thread(() -> {
                counter.incrementAndGet();
                latch.countDown();
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(parties, counter.get(), "所有线程都应该完成");
    }

    @Test
    void testCountDownLatchAwait() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        long start = System.currentTimeMillis();

        // 3个线程各sleep 500ms，但await是并行的
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;

        // 应该在500ms左右完成，不是1500ms
        assertTrue(elapsed >= 500 && elapsed < 800,
                "CountDownLatch 应该并行等待多个线程");
    }

    @Test
    void testCountDownLatchTimeout() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);

        // 只启动2个线程
        for (int i = 0; i < 2; i++) {
            new Thread(latch::countDown).start();
        }

        // 等待超时
        boolean completed = latch.await(1, TimeUnit.SECONDS);
        assertFalse(completed, "超时时应该返回 false");
        assertEquals(1, latch.getCount(), "计数器应该还剩1");
    }

    @Test
    void testCyclicBarrier() throws InterruptedException {
        int parties = 3;
        CyclicBarrier barrier = new CyclicBarrier(parties, () -> {
            // 所有线程到达时执行
        });
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch doneLatch = new CountDownLatch(parties);

        for (int i = 0; i < parties; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    Thread.sleep(threadId * 100);
                    barrier.await();
                    counter.incrementAndGet();
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        doneLatch.await(5, TimeUnit.SECONDS);
        assertEquals(parties, counter.get(), "所有线程都应该通过屏障");
    }

    @Test
    void testCyclicBarrierReuse() throws InterruptedException {
        CyclicBarrier barrier = new CyclicBarrier(3);
        AtomicInteger rounds = new AtomicInteger(0);
        CountDownLatch doneLatch = new CountDownLatch(6);

        for (int i = 0; i < 6; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    Thread.sleep(id * 50);
                    barrier.await();
                    if (id % 3 == 2) {
                        rounds.incrementAndGet();
                    }
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        doneLatch.await(5, TimeUnit.SECONDS);
        assertEquals(2, rounds.get(), "屏障应该被重用两次");
    }

    @Test
    void testSemaphore() throws InterruptedException {
        Semaphore semaphore = new Semaphore(2);
        AtomicInteger activeCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        CountDownLatch doneLatch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire();
                    int current = activeCount.incrementAndGet();
                    maxConcurrent.updateAndGet(max -> Math.max(max, current));
                    Thread.sleep(500);
                    activeCount.decrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release();
                    doneLatch.countDown();
                }
            }).start();
        }

        doneLatch.await(10, TimeUnit.SECONDS);
        assertTrue(maxConcurrent.get() <= 2,
                "Semaphore 应该限制并发数为2");
    }

    @Test
    void testSemaphoreFairness() throws InterruptedException {
        Semaphore unfairSemaphore = new Semaphore(1, false);
        Semaphore fairSemaphore = new Semaphore(1, true);

        // 测试非公平模式
        testSemaphoreFairness(unfairSemaphore, "非公平");

        // 测试公平模式
        testSemaphoreFairness(fairSemaphore, "公平");
    }

    private void testSemaphoreFairness(Semaphore semaphore, String mode) throws InterruptedException {
        AtomicInteger[] results = new AtomicInteger[3];
        CountDownLatch startLatch = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    if (id == 0) {
                        Thread.sleep(100); // 第一个线程晚一点
                    }
                    semaphore.acquire();
                    results[id] = new AtomicInteger((int) System.currentTimeMillis());
                    Thread.sleep(50);
                    semaphore.release();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        startLatch.countDown();
        Thread.sleep(500);
    }

    @Test
    void testExchanger() throws InterruptedException {
        Exchanger<String> exchanger = new Exchanger<>();
        CountDownLatch doneLatch = new CountDownLatch(2);
        String[] results = new String[2];

        Thread t1 = new Thread(() -> {
            try {
                results[0] = exchanger.exchange("来自线程1的数据");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                results[1] = exchanger.exchange("来自线程2的数据");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        t1.start();
        t2.start();
        doneLatch.await(5, TimeUnit.SECONDS);

        assertNotNull(results[0], "线程1应该收到数据");
        assertNotNull(results[1], "线程2应该收到数据");
        assertTrue(results[0].contains("线程2"), "线程1应该收到线程2的数据");
        assertTrue(results[1].contains("线程1"), "线程2应该收到线程1的数据");
    }

    @Test
    void testPhaser() throws InterruptedException {
        Phaser phaser = new Phaser(1); // 主线程注册
        int parties = 3;
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch doneLatch = new CountDownLatch(parties);

        // 注册其他线程
        phaser.bulkRegister(parties);

        for (int i = 0; i < parties; i++) {
            new Thread(() -> {
                try {
                    counter.incrementAndGet();
                    phaser.arriveAndAwaitAdvance();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // 主线程等待
        phaser.arriveAndAwaitAdvance();

        doneLatch.await(5, TimeUnit.SECONDS);
        assertEquals(parties, counter.get(), "所有线程都应该到达屏障");
    }
}
