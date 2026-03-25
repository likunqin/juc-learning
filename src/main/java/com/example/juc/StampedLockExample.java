package com.example.juc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.IntStream;

/**
 * StampedLock 学习示例
 * JDK 8 引入的读写锁，支持乐观读模式
 *
 * StampedLock 特点:
 * - 支持三种模式: 写锁、悲观读锁、乐观读锁
 * - 乐观读模式不需要阻塞写线程
 * - 适用于读多写少的场景
 * - 不支持可重入
 * - 比 ReentrantReadWriteLock 性能更好
 */
public class StampedLockExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== StampedLock 学习示例 ===\n");

        // 1. StampedLock 基本使用
        System.out.println("1. StampedLock 基本使用:");
        basicUsage();

        // 2. 乐观读模式
        System.out.println("\n2. 乐观读模式 (Optimistic Read):");
        optimisticRead();

        // 3. 三种锁模式对比
        System.out.println("\n3. 三种锁模式对比:");
        lockModesComparison();

        // 4. 乐观读失败重试
        System.out.println("\n4. 乐观读失败重试:");
        optimisticReadRetry();

        // 5. StampedLock vs ReentrantReadWriteLock 性能对比
        System.out.println("\n5. StampedLock vs ReentrantReadWriteLock 性能对比:");
        performanceComparison();

        // 6. StampedLock 读写转换
        System.out.println("\n6. StampedLock 读写转换:");
        lockConversion();

        // 7. StampedLock 注意事项
        System.out.println("\n7. StampedLock 注意事项:");
        stampedLockPitfalls();
    }

    // 1. StampedLock 基本使用
    private static void basicUsage() {
        StampedLock lock = new StampedLock();

        System.out.println("  StampedLock 三种模式:");
        System.out.println("    - writeLock(): 获取写锁 (排他锁)");
        System.out.println("    - readLock(): 获取读锁 (共享锁)");
        System.out.println("    - tryOptimisticRead(): 尝试乐观读 (不阻塞)");

        // 写锁示例
        long writeStamp = lock.writeLock();
        try {
            System.out.println("\n  获取写锁成功, stamp: " + writeStamp);
        } finally {
            lock.unlockWrite(writeStamp);
            System.out.println("  释放写锁");
        }

        // 读锁示例
        long readStamp = lock.readLock();
        try {
            System.out.println("  获取读锁成功, stamp: " + readStamp);
        } finally {
            lock.unlockRead(readStamp);
            System.out.println("  释放读锁");
        }
    }

    // 2. 乐观读模式
    private static void optimisticRead() throws InterruptedException {
        SharedData data = new SharedData();

        System.out.println("  乐观读模式特点:");
        System.out.println("    - 不阻塞其他读操作和写操作");
        System.out.println("    - 不加锁，直接读取数据");
        System.out.println("    - 读取后验证 stamp 是否变化");
        System.out.println("    - 如果验证失败，升级为悲观读锁重试");

        CountDownLatch latch = new CountDownLatch(1);

        // 写线程
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(50);
                data.update("新数据");
                System.out.println("  写线程更新数据");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        // 读线程 - 乐观读
        Thread reader = new Thread(() -> {
            try {
                long stamp = data.lock.tryOptimisticRead();
                String value = data.getValue();

                System.out.println("  乐观读: " + value + ", stamp: " + stamp);

                // 模拟一些处理
                Thread.sleep(100);

                // 验证 stamp
                if (!data.lock.validate(stamp)) {
                    System.out.println("  乐观读验证失败，数据被修改");
                    // 升级为悲观读锁
                    stamp = data.lock.readLock();
                    try {
                        value = data.getValue();
                        System.out.println("  悲观读重试: " + value);
                    } finally {
                        data.lock.unlockRead(stamp);
                    }
                } else {
                    System.out.println("  乐观读验证成功，数据未被修改");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        writer.start();
        reader.start();
        writer.join();
        reader.join();
    }

    // 3. 三种锁模式对比
    private static void lockModesComparison() {
        System.out.println("  模式对比表:");
        System.out.println("  ┌──────────────┬─────────────┬────────────┬──────────────┐");
        System.out.println("  │     模式     │   阻塞读   │   阻塞写   │    说明      │");
        System.out.println("  ├──────────────┼─────────────┼────────────┼──────────────┤");
        System.out.println("  │ writeLock()  │     是      │     是     │ 排他写锁     │");
        System.out.println("  │ readLock()   │     否      │     是     │ 共享读锁     │");
        System.out.println("  │ optimistic   │     否      │     否     │ 乐观读(不锁) │");
        System.out.println("  └──────────────┴─────────────┴────────────┴──────────────┘");

        System.out.println("\n  使用场景:");
        System.out.println("    - 乐观读: 读多写少，读取频率远高于修改频率");
        System.out.println("    - 读锁: 读多写少，但需要保证数据一致性");
        System.out.println("    - 写锁: 需要修改数据，或读后需要修改");
    }

    // 4. 乐观读失败重试
    private static void optimisticReadRetry() throws InterruptedException {
        Counter counter = new Counter();
        int readerCount = 5;
        int writerCount = 2;
        CountDownLatch latch = new CountDownLatch(readerCount + writerCount);

        System.out.println("  创建 " + readerCount + " 个读线程和 " + writerCount + " 个写线程:");

        // 读线程
        for (int i = 0; i < readerCount; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < 10; j++) {
                        int value = counter.get();
                        System.out.println("  读线程-" + id + ": " + value);
                        Thread.sleep(20);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 写线程
        for (int i = 0; i < writerCount; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < 5; j++) {
                        counter.increment();
                        System.out.println("  写线程-" + id + ": 增加计数");
                        Thread.sleep(100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        System.out.println("  最终值: " + counter.count);
    }

    // 5. StampedLock vs ReentrantReadWriteLock 性能对比
    private static void performanceComparison() throws InterruptedException {
        final int THREADS = 100;
        final int OPERATIONS = 1000;
        final int READ_RATIO = 90; // 90% 读操作

        System.out.println("  测试条件: " + THREADS + " 线程，每线程 " + OPERATIONS + " 操作");

        // StampedLock 测试
        StampedLockData slData = new StampedLockData();
        long slStart = System.nanoTime();
        CountDownLatch slLatch = new CountDownLatch(THREADS);

        for (int i = 0; i < THREADS; i++) {
            final int id = i;
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS; j++) {
                    if ((id + j) % 100 < READ_RATIO) {
                        slData.get();
                    } else {
                        slData.set("数据-" + j);
                    }
                }
                slLatch.countDown();
            }).start();
        }

        slLatch.await();
        long slTime = System.nanoTime() - slStart;

        // ReentrantReadWriteLock 测试
        ReadWriteLockData rwlData = new ReadWriteLockData();
        long rwlStart = System.nanoTime();
        CountDownLatch rwlLatch = new CountDownLatch(THREADS);

        for (int i = 0; i < THREADS; i++) {
            final int id = i;
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS; j++) {
                    if ((id + j) % 100 < READ_RATIO) {
                        rwlData.get();
                    } else {
                        rwlData.set("数据-" + j);
                    }
                }
                rwlLatch.countDown();
            }).start();
        }

        rwlLatch.await();
        long rwlTime = System.nanoTime() - rwlStart;

        System.out.println("  StampedLock: 耗时=" + (slTime / 1_000_000) + "ms");
        System.out.println("  ReentrantReadWriteLock: 耗时=" + (rwlTime / 1_000_000) + "ms");
        System.out.println("  性能提升: " + String.format("%.2f", (double) rwlTime / slTime) + "x");
        System.out.println("  结论: 读多写少场景下 StampedLock 性能更优");
    }

    // 6. StampedLock 读写转换
    private static void lockConversion() throws InterruptedException {
        ReadWriteCache cache = new ReadWriteCache();

        CountDownLatch latch = new CountDownLatch(1);

        // 线程1: 读后写
        Thread t1 = new Thread(() -> {
            long stamp = cache.lock.readLock();
            try {
                System.out.println("  线程1: 获取读锁, value=" + cache.value);
                Thread.sleep(100);

                // 读锁转写锁
                long writeStamp = cache.lock.tryConvertToWriteLock(stamp);
                if (writeStamp != 0) {
                    stamp = writeStamp;
                    System.out.println("  线程1: 读锁转写锁成功");
                    cache.value = "线程1修改";
                    System.out.println("  线程1: 修改后 value=" + cache.value);
                } else {
                    System.out.println("  线程1: 读锁转写锁失败");
                    cache.lock.unlockRead(stamp);
                    stamp = cache.lock.writeLock();
                    cache.value = "线程1修改";
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                cache.lock.unlockWrite(stamp);
                latch.countDown();
            }
        });

        t1.start();
        t1.join();
    }

    // 7. StampedLock 注意事项
    private static void stampedLockPitfalls() {
        System.out.println("  StampedLock 限制和注意事项:");
        System.out.println("    1. 不支持可重入");
        System.out.println("       - 同一线程不能重复获取同一把锁");
        System.out.println("       - 重复获取会导致死锁");
        System.out.println();
        System.out.println("    2. 乐观读可能不一致");
        System.out.println("       - 乐观读不加锁，可能读到不一致的数据");
        System.out.println("       - 必须验证 stamp，失败需重试");
        System.out.println();
        System.out.println("    3. 锁转换可能失败");
        System.out.println("       - tryConvertToWriteLock() 可能返回 0");
        System.out.println("       - 需要处理转换失败的情况");
        System.out.println();
        System.out.println("    4. 不可中断");
        System.out.println("       - 获取锁的操作不可中断");
        System.out.println("       - 可使用 tryLock 带超时");
        System.out.println();
        System.out.println("    5. 不支持 Condition");
        System.out.println("       - StampedLock 没有 Condition");
        System.out.println("       - 需要条件变量请使用 ReentrantLock");
    }

    // 共享数据类
    static class SharedData {
        final StampedLock lock = new StampedLock();
        private String value = "初始数据";

        public String getValue() {
            long stamp = lock.tryOptimisticRead();
            String result = value;
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    result = value;
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            return result;
        }

        public void update(String newValue) {
            long stamp = lock.writeLock();
            try {
                this.value = newValue;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
    }

    // 线程安全计数器
    static class Counter {
        final StampedLock lock = new StampedLock();
        long count = 0;

        public int get() {
            long stamp = lock.tryOptimisticRead();
            long result = count;
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    result = count;
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            return (int) result;
        }

        public void increment() {
            long stamp = lock.writeLock();
            try {
                count++;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
    }

    // StampedLock 数据
    static class StampedLockData {
        final StampedLock lock = new StampedLock();
        private String value = "初始值";

        public String get() {
            long stamp = lock.tryOptimisticRead();
            String result = value;
            if (!lock.validate(stamp)) {
                stamp = lock.readLock();
                try {
                    result = value;
                } finally {
                    lock.unlockRead(stamp);
                }
            }
            return result;
        }

        public void set(String value) {
            long stamp = lock.writeLock();
            try {
                this.value = value;
            } finally {
                lock.unlockWrite(stamp);
            }
        }
    }

    // ReentrantReadWriteLock 数据
    static class ReadWriteLockData {
        private final java.util.concurrent.locks.ReadWriteLock lock =
            new java.util.concurrent.locks.ReentrantReadWriteLock();
        private String value = "初始值";

        public String get() {
            lock.readLock().lock();
            try {
                return value;
            } finally {
                lock.readLock().unlock();
            }
        }

        public void set(String value) {
            lock.writeLock().lock();
            try {
                this.value = value;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    // 读写转换缓存
    static class ReadWriteCache {
        final StampedLock lock = new StampedLock();
        String value = "初始值";
    }
}
