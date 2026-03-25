package com.example.juc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 锁机制单元测试
 */
class LockExampleTest {

    @Test
    void testReentrantLockBasic() throws InterruptedException {
        Lock lock = new ReentrantLock();
        AtomicInteger counter = new AtomicInteger(0);
        int threads = 10;
        int incrementsPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        lock.lock();
                        try {
                            counter.incrementAndGet();
                        } finally {
                            lock.unlock();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);
        assertEquals(threads * incrementsPerThread, counter.get(),
                "ReentrantLock 应该保证线程安全");
    }

    @Test
    void testReentrantLockTryLock() throws InterruptedException {
        Lock lock = new ReentrantLock();
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            lock.lock();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
                latch.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                    try {
                        counter.incrementAndGet();
                    } finally {
                        lock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            latch.countDown();
        });

        t1.start();
        Thread.sleep(100);
        t2.start();

        latch.await(3, TimeUnit.SECONDS);
        assertEquals(0, counter.get(), "tryLock 超时后应该不执行");
    }

    @Test
    void testReentrantLockReentrancy() {
        ReentrantLock lock = new ReentrantLock();
        AtomicInteger depth = new AtomicInteger(0);

        Runnable recursive = () -> {
            if (lock.getHoldCount() < 3) {
                lock.lock();
                try {
                    int currentDepth = lock.getHoldCount();
                    depth.set(currentDepth);
                    recursive.run();
                } finally {
                    lock.unlock();
                }
            }
        };

        lock.lock();
        try {
            recursive.run();
        } finally {
            lock.unlock();
        }

        assertEquals(3, depth.get(), "ReentrantLock 应该支持重入");
    }

    @Test
    void testConditionSignal() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        AtomicInteger value = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        Thread waiter = new Thread(() -> {
            lock.lock();
            try {
                while (value.get() == 0) {
                    condition.await();
                }
                assertEquals(42, value.get(), "应该接收到正确的值");
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });

        Thread signaler = new Thread(() -> {
            lock.lock();
            try {
                Thread.sleep(500);
                value.set(42);
                condition.signal();
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });

        waiter.start();
        signaler.start();

        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    void testConditionSignalAll() throws InterruptedException {
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        AtomicInteger value = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(4);

        // 3个等待线程
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                lock.lock();
                try {
                    while (value.get() == 0) {
                        condition.await();
                    }
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }).start();
        }

        // 信号线程
        new Thread(() -> {
            lock.lock();
            try {
                Thread.sleep(500);
                value.set(42);
                condition.signalAll();
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }).start();

        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    void testReadWriteLock() throws InterruptedException {
        java.util.concurrent.locks.ReadWriteLock rwLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
        AtomicInteger readers = new AtomicInteger(0);
        AtomicInteger writers = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(5);

        // 读线程
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    rwLock.readLock().lock();
                    int currentReaders = readers.incrementAndGet();
                    Thread.sleep(200);
                    assertTrue(currentReaders <= 3, "最多3个读线程同时持有锁");
                    readers.decrementAndGet();
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    rwLock.readLock().unlock();
                }
            }).start();
        }

        // 写线程
        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    rwLock.writeLock().lock();
                    int currentWriters = writers.incrementAndGet();
                    Thread.sleep(100);
                    assertEquals(1, currentWriters, "写锁应该是独占的");
                    writers.decrementAndGet();
                    latch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    rwLock.writeLock().unlock();
                }
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);
    }
}
