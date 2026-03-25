package com.example.juc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Volatile 单元测试
 */
class VolatileExampleTest {

    @Test
    void testVolatileVisibility() throws InterruptedException {
        class SharedData {
            private volatile boolean flag = false;
            private int value = 0;

            public void setFlagAndValue(boolean flag, int value) {
                this.value = value;
                this.flag = flag;
            }

            public boolean getFlag() {
                return flag;
            }

            public int getValue() {
                return value;
            }
        }

        SharedData data = new SharedData();
        AtomicInteger result = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        Thread reader = new Thread(() -> {
            while (!data.getFlag()) {
                // 自旋等待
            }
            result.set(data.getValue());
            latch.countDown();
        });

        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(100);
                data.setFlagAndValue(true, 42);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        reader.start();
        writer.start();

        latch.await(5, TimeUnit.SECONDS);

        // 由于 volatile 的 happens-before 关系，应该能看到 value = 42
        assertEquals(42, result.get(),
                "volatile 应该保证可见性和有序性");
    }

    @Test
    void testVolatileNotAtomic() throws InterruptedException {
        class VolatileCounter {
            private volatile int counter = 0;

            public void increment() {
                counter++; // 复合操作，非原子
            }

            public int get() {
                return counter;
            }
        }

        VolatileCounter counter = new VolatileCounter();
        int threads = 10;
        int increments = 1000;
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < increments; j++) {
                    counter.increment();
                }
                latch.countDown();
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);

        // volatile 不能保证复合操作的原子性
        // 结果可能小于预期
        assertTrue(counter.get() <= threads * increments,
                "volatile 不能保证复合操作的原子性");
    }

    @Test
    void testDoubleCheckedLocking() throws InterruptedException {
        class Singleton {
            private static volatile Singleton instance;
            private int value;

            private Singleton() {
                this.value = 42;
            }

            public static Singleton getInstance() {
                if (instance == null) { // 第一次检查
                    synchronized (Singleton.class) {
                        if (instance == null) { // 第二次检查
                            instance = new Singleton();
                        }
                    }
                }
                return instance;
            }

            public int getValue() {
                return value;
            }
        }

        CountDownLatch latch = new CountDownLatch(10);
        Singleton[] instances = new Singleton[10];

        for (int i = 0; i < 10; i++) {
            final int index = i;
            new Thread(() -> {
                instances[index] = Singleton.getInstance();
                latch.countDown();
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);

        // 所有线程应该获得同一个实例
        Singleton first = instances[0];
        for (Singleton instance : instances) {
            assertSame(first, instance,
                    "double-checked locking 应该保证只创建一个实例");
        }

        // 实例应该是完全初始化的
        assertEquals(42, first.getValue(),
                "volatile 应该防止指令重排序");
    }

    @Test
    void testVolatileWithAtomicOperations() throws InterruptedException {
        class VolatileWithAtomic {
            private volatile int value = 0;

            public synchronized void increment() {
                value++;
            }

            public int get() {
                return value;
            }
        }

        VolatileWithAtomic counter = new VolatileWithAtomic();
        int threads = 10;
        int increments = 1000;
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < increments; j++) {
                    counter.increment();
                }
                latch.countDown();
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);

        // synchronized 保证了原子性
        assertEquals(threads * increments, counter.get(),
                "synchronized + volatile 应该保证正确性");
    }
}
