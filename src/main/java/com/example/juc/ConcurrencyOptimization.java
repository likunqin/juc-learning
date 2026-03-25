package com.example.juc;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.stream.*;

/**
 * 并发性能优化学习示例
 * 演示各种优化技巧和最佳实践
 */
public class ConcurrencyOptimization {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 并发性能优化学习示例 ===\n");

        // 1. 锁粒度优化
        System.out.println("1. 锁粒度优化:");
        lockGranularityOptimization();

        // 2. 读写分离优化
        System.out.println("\n2. 读写分离优化:");
        readWriteOptimization();

        // 3. 使用 LongAdder 替代 AtomicLong
        System.out.println("\n3. LongAdder vs AtomicLong:");
        longAdderOptimization();

        // 4. 批量操作优化
        System.out.println("\n4. 批量操作优化:");
        batchOperationOptimization();

        // 5. 并行流优化
        System.out.println("\n5. 并行流优化:");
        parallelStreamOptimization();

        // 6. 减少锁竞争 - 锁分段
        System.out.println("\n6. 锁分段技术:");
        stripingOptimization();

        // 7. 对象复用优化
        System.out.println("\n7. 对象复用优化:");
        objectReuseOptimization();

        // 8. 减少上下文切换
        System.out.println("\n8. 减少上下文切换:");
        contextSwitchOptimization();

        // 9. 异步处理优化
        System.out.println("\n9. 异步处理优化:");
        asyncOptimization();
    }

    // 1. 锁粒度优化
    private static void lockGranularityOptimization() throws InterruptedException {
        System.out.println("  示例: 粗粒度锁 vs 细粒度锁");

        final int THREADS = 10;
        final int OPERATIONS = 100_000;

        // 粗粒度锁 - 整个对象一把锁
        CoarseLockedMap coarseMap = new CoarseLockedMap();
        long coarseStart = System.nanoTime();
        CountDownLatch coarseLatch = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            final int id = i;
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS; j++) {
                    coarseMap.put("key" + ((id * OPERATIONS + j) % 100), "value");
                }
                coarseLatch.countDown();
            }).start();
        }
        coarseLatch.await();
        long coarseTime = System.nanoTime() - coarseStart;

        // 细粒度锁 - 每个桶一把锁
        FineLockedMap fineMap = new FineLockedMap();
        long fineStart = System.nanoTime();
        CountDownLatch fineLatch = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            final int id = i;
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS; j++) {
                    fineMap.put("key" + ((id * OPERATIONS + j) % 100), "value");
                }
                fineLatch.countDown();
            }).start();
        }
        fineLatch.await();
        long fineTime = System.nanoTime() - fineStart;

        System.out.println("  粗粒度锁耗时: " + (coarseTime / 1_000_000) + "ms");
        System.out.println("  细粒度锁耗时: " + (fineTime / 1_000_000) + "ms");
        System.out.println("  性能提升: " + String.format("%.2f", (double) coarseTime / fineTime) + "x");
    }

    // 2. 读写分离优化
    private static void readWriteOptimization() throws InterruptedException {
        final int THREADS = 20;
        final int OPERATIONS = 50_000;

        // 使用 synchronized
        SynchronizedData syncData = new SynchronizedData();
        long syncStart = System.nanoTime();
        CountDownLatch syncLatch = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            final boolean isWrite = i < 5; // 5个写线程，15个读线程
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS; j++) {
                    if (isWrite) {
                        syncData.set(j);
                    } else {
                        syncData.get();
                    }
                }
                syncLatch.countDown();
            }).start();
        }
        syncLatch.await();
        long syncTime = System.nanoTime() - syncStart;

        // 使用 ReadWriteLock
        ReadWriteLockData rwData = new ReadWriteLockData();
        long rwStart = System.nanoTime();
        CountDownLatch rwLatch = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            final boolean isWrite = i < 5;
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS; j++) {
                    if (isWrite) {
                        rwData.set(j);
                    } else {
                        rwData.get();
                    }
                }
                rwLatch.countDown();
            }).start();
        }
        rwLatch.await();
        long rwTime = System.nanoTime() - rwStart;

        System.out.println("  synchronized 耗时: " + (syncTime / 1_000_000) + "ms");
        System.out.println("  ReadWriteLock 耗时: " + (rwTime / 1_000_000) + "ms");
        System.out.println("  性能提升: " + String.format("%.2f", (double) syncTime / rwTime) + "x");
    }

    // 3. LongAdder vs AtomicLong
    private static void longAdderOptimization() throws InterruptedException {
        final int THREADS = 50;
        final int INCREMENTS = 100_000;

        // AtomicLong
        AtomicLong atomicLong = new AtomicLong(0);
        long alStart = System.nanoTime();
        CountDownLatch alLatch = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    atomicLong.incrementAndGet();
                }
                alLatch.countDown();
            }).start();
        }
        alLatch.await();
        long alTime = System.nanoTime() - alStart;

        // LongAdder
        LongAdder longAdder = new LongAdder();
        long laStart = System.nanoTime();
        CountDownLatch laLatch = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    longAdder.increment();
                }
                laLatch.countDown();
            }).start();
        }
        laLatch.await();
        long laTime = System.nanoTime() - laStart;

        System.out.println("  AtomicLong: " + atomicLong.get() + ", 耗时=" + (alTime / 1_000_000) + "ms");
        System.out.println("  LongAdder: " + longAdder.sum() + ", 耗时=" + (laTime / 1_000_000) + "ms");
        System.out.println("  性能提升: " + String.format("%.2f", (double) alTime / laTime) + "x");
        System.out.println("  适用场景: LongAdder 适合高并发计数，AtomicLong 适合需要精确值时");
    }

    // 4. 批量操作优化
    private static void batchOperationOptimization() throws InterruptedException {
        final int THREADS = 10;
        final int ITEMS_PER_THREAD = 1000;

        LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>(THREADS * ITEMS_PER_THREAD);

        // 单个添加
        long singleStart = System.nanoTime();
        CountDownLatch singleLatch = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            final int id = i;
            new Thread(() -> {
                for (int j = 0; j < ITEMS_PER_THREAD; j++) {
                    queue.offer(id * ITEMS_PER_THREAD + j);
                }
                singleLatch.countDown();
            }).start();
        }
        singleLatch.await();
        long singleTime = System.nanoTime() - singleStart;

        // 清空队列
        queue.clear();

        // 批量添加
        long batchStart = System.nanoTime();
        CountDownLatch batchLatch = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            final int id = i;
            new Thread(() -> {
                List<Integer> batch = new ArrayList<>();
                for (int j = 0; j < ITEMS_PER_THREAD; j++) {
                    batch.add(id * ITEMS_PER_THREAD + j);
                    if (batch.size() == 100) {
                        queue.addAll(batch);
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    queue.addAll(batch);
                }
                batchLatch.countDown();
            }).start();
        }
        batchLatch.await();
        long batchTime = System.nanoTime() - batchStart;

        System.out.println("  单个添加耗时: " + (singleTime / 1_000_000) + "ms");
        System.out.println("  批量添加耗时: " + (batchTime / 1_000_000) + "ms");
        System.out.println("  性能提升: " + String.format("%.2f", (double) singleTime / batchTime) + "x");
    }

    // 5. 并行流优化
    private static void parallelStreamOptimization() {
        final int SIZE = 10_000_000;
        List<Integer> data = IntStream.range(0, SIZE).boxed().collect(Collectors.toList());

        System.out.println("  数据量: " + SIZE);

        // 串行流
        long seqStart = System.nanoTime();
        long seqSum = data.stream()
            .mapToLong(x -> x)
            .filter(x -> x % 2 == 0)
            .sum();
        long seqTime = System.nanoTime() - seqStart;

        // 并行流
        long parStart = System.nanoTime();
        long parSum = data.parallelStream()
            .mapToLong(x -> x)
            .filter(x -> x % 2 == 0)
            .sum();
        long parTime = System.nanoTime() - parStart;

        System.out.println("  串行流: 和=" + seqSum + ", 耗时=" + (seqTime / 1_000_000) + "ms");
        System.out.println("  并行流: 和=" + parSum + ", 耗时=" + (parTime / 1_000_000) + "ms");
        System.out.println("  性能提升: " + String.format("%.2f", (double) seqTime / parTime) + "x");
        System.out.println("  线程数: " + ForkJoinPool.commonPool().getParallelism());
        System.out.println("  提示: 并行流适合 CPU 密集型、大数据量操作");
    }

    // 6. 锁分段技术
    private static void stripingOptimization() throws InterruptedException {
        final int THREADS = 20;
        final int OPERATIONS = 100_000;

        // 不分段
        StripedCounter plainCounter = new StripedCounter(1);
        long plainStart = System.nanoTime();
        CountDownLatch plainLatch = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS; j++) {
                    plainCounter.increment(j);
                }
                plainLatch.countDown();
            }).start();
        }
        plainLatch.await();
        long plainTime = System.nanoTime() - plainStart;

        // 分段锁 (16 段)
        StripedCounter stripedCounter = new StripedCounter(16);
        long stripedStart = System.nanoTime();
        CountDownLatch stripedLatch = new CountDownLatch(THREADS);
        for (int i = 0; i < THREADS; i++) {
            new Thread(() -> {
                for (int j = 0; j < OPERATIONS; j++) {
                    stripedCounter.increment(j);
                }
                stripedLatch.countDown();
            }).start();
        }
        stripedLatch.await();
        long stripedTime = System.nanoTime() - stripedStart;

        System.out.println("  不分段耗时: " + (plainTime / 1_000_000) + "ms");
        System.out.println("  16段分段耗时: " + (stripedTime / 1_000_000) + "ms");
        System.out.println("  性能提升: " + String.format("%.2f", (double) plainTime / stripedTime) + "x");
    }

    // 7. 对象复用优化
    private static void objectReuseOptimization() {
        final int ITERATIONS = 100_000;

        // 每次创建新对象
        long createStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            String result = new StringBuilder()
                .append("Prefix-")
                .append(i)
                .append("-Suffix")
                .toString();
        }
        long createTime = System.nanoTime() - createStart;

        // 复用对象
        long reuseStart = System.nanoTime();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ITERATIONS; i++) {
            sb.setLength(0);
            sb.append("Prefix-");
            sb.append(i);
            sb.append("-Suffix");
            String result = sb.toString();
        }
        long reuseTime = System.nanoTime() - reuseStart;

        System.out.println("  创建新对象耗时: " + (createTime / 1_000_000) + "ms");
        System.out.println("  复用对象耗时: " + (reuseTime / 1_000_000) + "ms");
        System.out.println("  性能提升: " + String.format("%.2f", (double) createTime / reuseTime) + "x");
        System.out.println("  注意: 对象复用在多线程环境下需要考虑线程安全");
    }

    // 8. 减少上下文切换
    private static void contextSwitchOptimization() throws InterruptedException {
        final int TASKS = 1000;

        // 方案1: 每个任务一个线程
        long manyThreadsStart = System.nanoTime();
        CountDownLatch manyLatch = new CountDownLatch(TASKS);
        ExecutorService manyPool = Executors.newCachedThreadPool();
        for (int i = 0; i < TASKS; i++) {
            final int id = i;
            manyPool.submit(() -> {
                int result = id * 2;
                manyLatch.countDown();
            });
        }
        manyLatch.await();
        manyPool.shutdown();
        long manyThreadsTime = System.nanoTime() - manyThreadsStart;

        // 方案2: 使用固定线程池
        long fixedPoolStart = System.nanoTime();
        CountDownLatch fixedLatch = new CountDownLatch(TASKS);
        ExecutorService fixedPool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < TASKS; i++) {
            final int id = i;
            fixedPool.submit(() -> {
                int result = id * 2;
                fixedLatch.countDown();
            });
        }
        fixedLatch.await();
        fixedPool.shutdown();
        long fixedPoolTime = System.nanoTime() - fixedPoolStart;

        System.out.println("  缓存线程池耗时: " + (manyThreadsTime / 1_000_000) + "ms");
        System.out.println("  固定线程池(8)耗时: " + (fixedPoolTime / 1_000_000) + "ms");
        System.out.println("  性能提升: " + String.format("%.2f", (double) manyThreadsTime / fixedPoolTime) + "x");
        System.out.println("  建议: 根据任务类型设置合理的线程池大小");
    }

    // 9. 异步处理优化
    private static void asyncOptimization() throws Exception {
        System.out.println("  示例: 同步 vs 异步处理");

        // 同步处理
        long syncStart = System.nanoTime();
        int result1 = simulateApiCall(100);
        int result2 = simulateApiCall(150);
        int result3 = simulateApiCall(80);
        int syncTotal = result1 + result2 + result3;
        long syncTime = System.nanoTime() - syncStart;

        // 异步处理
        long asyncStart = System.nanoTime();
        CompletableFuture<Integer> f1 = CompletableFuture.supplyAsync(() -> simulateApiCall(100));
        CompletableFuture<Integer> f2 = CompletableFuture.supplyAsync(() -> simulateApiCall(150));
        CompletableFuture<Integer> f3 = CompletableFuture.supplyAsync(() -> simulateApiCall(80));
        int asyncTotal = f1.get() + f2.get() + f3.get();
        long asyncTime = System.nanoTime() - asyncStart;

        System.out.println("  同步处理: 总和=" + syncTotal + ", 耗时=" + (syncTime / 1_000_000) + "ms");
        System.out.println("  异步处理: 总和=" + asyncTotal + ", 耗时=" + (asyncTime / 1_000_000) + "ms");
        System.out.println("  性能提升: " + String.format("%.2f", (double) syncTime / asyncTime) + "x");
        System.out.println("  适用场景: 异步处理适合 IO 密集型、独立任务");
    }

    private static int simulateApiCall(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return ms;
    }

    // ============ 测试类 ============

    static class CoarseLockedMap {
        private final Map<String, String> map = new HashMap<>();
        private final Object lock = new Object();

        public void put(String key, String value) {
            synchronized (lock) {
                map.put(key, value);
            }
        }
    }

    static class FineLockedMap {
        private final List<Map<String, String>> buckets = new ArrayList<>();
        private final List<Object> locks = new ArrayList<>();
        private final int BUCKET_COUNT = 16;

        public FineLockedMap() {
            for (int i = 0; i < BUCKET_COUNT; i++) {
                buckets.add(new HashMap<>());
                locks.add(new Object());
            }
        }

        public void put(String key, String value) {
            int bucketIndex = Math.abs(key.hashCode()) % BUCKET_COUNT;
            synchronized (locks.get(bucketIndex)) {
                buckets.get(bucketIndex).put(key, value);
            }
        }
    }

    static class SynchronizedData {
        private int value = 0;

        public synchronized int get() {
            return value;
        }

        public synchronized void set(int value) {
            this.value = value;
        }
    }

    static class ReadWriteLockData {
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private int value = 0;

        public int get() {
            lock.readLock().lock();
            try {
                return value;
            } finally {
                lock.readLock().unlock();
            }
        }

        public void set(int value) {
            lock.writeLock().lock();
            try {
                this.value = value;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    static class StripedCounter {
        private final LongAdder[] counters;
        private final int stripes;

        public StripedCounter(int stripes) {
            this.stripes = stripes;
            this.counters = new LongAdder[stripes];
            for (int i = 0; i < stripes; i++) {
                counters[i] = new LongAdder();
            }
        }

        public void increment(int key) {
            int stripe = Math.abs(key) % stripes;
            counters[stripe].increment();
        }
    }
}
