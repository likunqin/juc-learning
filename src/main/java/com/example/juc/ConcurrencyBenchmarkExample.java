package com.example.juc;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * 并发性能基准测试示例
 * <p>
 * 演示如何进行并发代码的性能测试和对比
 * </p>
 *
 * <h3>性能测试要点：</h3>
 * <ul>
 *   <li>预热 - JVM 需要预热才能达到最佳性能</li>
 *   <li>多次运行 - 单次运行结果不可靠</li>
 *   <li>统计指标 - 平均值、中位数、标准差</li>
   <li>排除干扰 - 关闭 GC、系统负载等</li>
 * </ul>
 *
 * <h3>推荐工具：</h3>
 * <ul>
 *   <li>JMH - Java Microbenchmark Harness</li>
 *   <li>JMeter - 压力测试工具</li>
 *   <li>自定义基准测试（简单场景）</li>
 * </ul>
 */
public class ConcurrencyBenchmarkExample {

    /**
     * 场景1：synchronized vs ReentrantLock 性能对比
     * <p>
     * 对比两种锁的性能差异
     * </p>
     */
    public static void synchronizedVsReentrantLock() {
        System.out.println("=== 场景1：synchronized vs ReentrantLock ===");

        class SynchronizedCounter {
            private int counter = 0;

            public synchronized void increment() {
                counter++;
            }

            public int get() {
                return counter;
            }
        }

        class LockCounter {
            private int counter = 0;
            private final Lock lock = new ReentrantLock();

            public void increment() {
                lock.lock();
                try {
                    counter++;
                } finally {
                    lock.unlock();
                }
            }

            public int get() {
                lock.lock();
                try {
                    return counter;
                } finally {
                    lock.unlock();
                }
            }
        }

        int threads = 10;
        int iterations = 1000000;
        int warmup = 5;

        // 预热
        System.out.println("预热中...");
        for (int i = 0; i < warmup; i++) {
            testCounter(new SynchronizedCounter(), threads, iterations);
            testCounter(new LockCounter(), threads, iterations);
        }

        // 正式测试
        System.out.println("\n开始正式测试:");

        List<Long> syncTimes = new ArrayList<>();
        List<Long> lockTimes = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            syncTimes.add(testCounter(new SynchronizedCounter(), threads, iterations));
            lockTimes.add(testCounter(new LockCounter(), threads, iterations));
        }

        System.out.println("\nsynchronized: " + formatStats(syncTimes) + " ms");
        System.out.println("ReentrantLock: " + formatStats(lockTimes) + " ms");
        System.out.println("差异: " + compare(syncTimes, lockTimes));
    }

    /**
     * 场景2：HashMap vs ConcurrentHashMap 性能对比
     * <p>
     * 对比两种 Map 的读写性能
     * </p>
     */
    public static void mapComparison() {
        System.out.println("\n=== 场景2：HashMap vs ConcurrentHashMap ===");

        int threads = 10;
        int operations = 100000;
        int warmup = 3;

        // 预热
        System.out.println("预热中...");
        for (int i = 0; i < warmup; i++) {
            testMap(new HashMap<>(), threads, operations);
            testMap(new ConcurrentHashMap<>(), threads, operations);
        }

        // 正式测试
        System.out.println("\n开始正式测试:");

        List<Long> hashMapTimes = new ArrayList<>();
        List<Long> concurrentMapTimes = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            hashMapTimes.add(testMap(new HashMap<>(), threads, operations));
            concurrentMapTimes.add(testMap(new ConcurrentHashMap<>(), threads, operations));
        }

        System.out.println("\nHashMap (同步): " + formatStats(hashMapTimes) + " ms");
        System.out.println("ConcurrentHashMap: " + formatStats(concurrentMapTimes) + " ms");
        System.out.println("差异: " + compare(concurrentMapTimes, hashMapTimes));
    }

    /**
     * 场景3：AtomicInteger vs synchronized 计数器对比
     * <p>
     * 对比原子类和同步方式的性能
     * </p>
     */
    public static void atomicVsSynchronizedCounter() {
        System.out.println("\n=== 场景3：AtomicInteger vs synchronized ===");

        int threads = 20;
        int iterations = 1000000;
        int warmup = 5;

        class SyncCounter {
            private int counter = 0;

            public synchronized void increment() {
                counter++;
            }

            public int get() {
                return counter;
            }
        }

        // 预热
        System.out.println("预热中...");
        for (int i = 0; i < warmup; i++) {
            testAtomicCounter(threads, iterations);
            testCounter(new SyncCounter(), threads, iterations);
        }

        // 正式测试
        System.out.println("\n开始正式测试:");

        List<Long> atomicTimes = new ArrayList<>();
        List<Long> syncTimes = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            atomicTimes.add(testAtomicCounter(threads, iterations));
            syncTimes.add(testCounter(new SyncCounter(), threads, iterations));
        }

        System.out.println("\nAtomicInteger: " + formatStats(atomicTimes) + " ms");
        System.out.println("synchronized: " + formatStats(syncTimes) + " ms");
        System.out.println("差异: " + compare(atomicTimes, syncTimes));
    }

    /**
     * 场景4：不同 BlockingQueue 性能对比
     * <p>
     * 对比不同类型的阻塞队列
     * </p>
     */
    public static void blockingQueueComparison() throws InterruptedException {
        System.out.println("\n=== 场景4：BlockingQueue 性能对比 ===");

        int threads = 10;
        int items = 100000;
        int warmup = 3;

        // 预热
        System.out.println("预热中...");
        for (int i = 0; i < warmup; i++) {
            testQueue(new ArrayBlockingQueue<>(1000), threads, items);
            testQueue(new LinkedBlockingQueue<>(1000), threads, items);
            testQueue(new LinkedBlockingDeque<>(1000), threads, items);
        }

        // 正式测试
        System.out.println("\n开始正式测试:");

        List<Long> arrayQueueTimes = new ArrayList<>();
        List<Long> linkedQueueTimes = new ArrayList<>();
        List<Long> linkedDequeTimes = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            arrayQueueTimes.add(testQueue(new ArrayBlockingQueue<>(1000), threads, items));
            linkedQueueTimes.add(testQueue(new LinkedBlockingQueue<>(1000), threads, items));
            linkedDequeTimes.add(testQueue(new LinkedBlockingDeque<>(1000), threads, items));
        }

        System.out.println("\nArrayBlockingQueue: " + formatStats(arrayQueueTimes) + " ms");
        System.out.println("LinkedBlockingQueue: " + formatStats(linkedQueueTimes) + " ms");
        System.out.println("LinkedBlockingDeque: " + formatStats(linkedDequeTimes) + " ms");
    }

    /**
     * 场景5：不同线程池配置性能对比
     * <p>
     * 对比不同线程池配置的性能
     * </p>
     */
    public static void threadPoolComparison() throws InterruptedException {
        System.out.println("\n=== 场景5：线程池配置对比 ===");

        int tasks = 1000;
        int taskDuration = 10; // ms
        int warmup = 3;

        // 预热
        System.out.println("预热中...");
        for (int i = 0; i < warmup; i++) {
            testThreadPool(2, tasks, taskDuration);
            testThreadPool(4, tasks, taskDuration);
            testThreadPool(8, tasks, taskDuration);
        }

        // 正式测试
        System.out.println("\n开始正式测试:");

        List<Long> pool2Times = new ArrayList<>();
        List<Long> pool4Times = new ArrayList<>();
        List<Long> pool8Times = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            pool2Times.add(testThreadPool(2, tasks, taskDuration));
            pool4Times.add(testThreadPool(4, tasks, taskDuration));
            pool8Times.add(testThreadPool(8, tasks, taskDuration));
        }

        System.out.println("\n线程数=2: " + formatStats(pool2Times) + " ms");
        System.out.println("线程数=4: " + formatStats(pool4Times) + " ms");
        System.out.println("线程数=8: " + formatStats(pool8Times) + " ms");
    }

    /**
     * 场景6：LongAdder vs AtomicLong 性能对比
     * <p>
     * 对比在高并发场景下的计数器性能
     * </p>
     */
    public static void longAdderVsAtomicLong() {
        System.out.println("\n=== 场景6：LongAdder vs AtomicLong ===");

        int threads = 20;
        long iterations = 10000000;
        int warmup = 5;

        // 预热
        System.out.println("预热中...");
        for (int i = 0; i < warmup; i++) {
            testLongAdder(threads, iterations);
            testAtomicLong(threads, iterations);
        }

        // 正式测试
        System.out.println("\n开始正式测试:");

        List<Long> adderTimes = new ArrayList<>();
        List<Long> atomicTimes = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            adderTimes.add(testLongAdder(threads, iterations));
            atomicTimes.add(testAtomicLong(threads, iterations));
        }

        System.out.println("\nLongAdder: " + formatStats(adderTimes) + " ms");
        System.out.println("AtomicLong: " + formatStats(atomicTimes) + " ms");
        System.out.println("差异: " + compare(adderTimes, atomicTimes));
    }

    /**
     * 场景7：String vs StringBuilder vs StringBuffer
     * <p>
     * 对比不同字符串拼接方式的性能
     * </p>
     */
    public static void stringComparison() {
        System.out.println("\n=== 场景7：字符串拼接对比 ===");

        int threads = 10;
        int operations = 10000;
        int warmup = 3;

        // 预热
        System.out.println("预热中...");
        for (int i = 0; i < warmup; i++) {
            testStringBuilder(threads, operations);
            testStringBuffer(threads, operations);
        }

        // 正式测试
        System.out.println("\n开始正式测试:");

        List<Long> builderTimes = new ArrayList<>();
        List<Long> bufferTimes = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            builderTimes.add(testStringBuilder(threads, operations));
            bufferTimes.add(testStringBuffer(threads, operations));
        }

        System.out.println("\nStringBuilder (非线程安全): " + formatStats(builderTimes) + " ms");
        System.out.println("StringBuffer (线程安全): " + formatStats(bufferTimes) + " ms");
        System.out.println("差异: " + compare(builderTimes, bufferTimes));
    }

    /**
     * 辅助测试方法
     */
    private static long testCounter(Object counter, int threads, int iterations) {
        CountDownLatch latch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations; j++) {
                        if (counter instanceof SynchronizedCounter) {
                            ((SynchronizedCounter) counter).increment();
                        } else {
                            ((LockCounter) counter).increment();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        long start = System.currentTimeMillis();
        startLatch.countDown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return System.currentTimeMillis() - start;
    }

    private static long testMap(Map<Integer, Integer> map, int threads, int operations) {
        CountDownLatch latch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    Random random = new Random();
                    for (int j = 0; j < operations; j++) {
                        int key = random.nextInt(10000);
                        if (map instanceof ConcurrentHashMap) {
                            ((ConcurrentHashMap<Integer, Integer>) map).computeIfAbsent(key, k -> k);
                        } else {
                            synchronized (map) {
                                map.putIfAbsent(key, key);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        long start = System.currentTimeMillis();
        startLatch.countDown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return System.currentTimeMillis() - start;
    }

    private static long testAtomicCounter(int threads, int iterations) {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations; j++) {
                        counter.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        long start = System.currentTimeMillis();
        startLatch.countDown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return System.currentTimeMillis() - start;
    }

    private static long testQueue(BlockingQueue<Integer> queue, int threads, int items) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(threads * 2);
        CountDownLatch startLatch = new CountDownLatch(1);

        // 生产者线程
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < items; j++) {
                        queue.put(j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        // 消费者线程
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < items; j++) {
                        queue.take();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        long start = System.currentTimeMillis();
        startLatch.countDown();
        latch.await();
        return System.currentTimeMillis() - start;
    }

    private static long testThreadPool(int poolSize, int tasks, int taskDuration) throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                poolSize, poolSize,
                0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );

        CountDownLatch latch = new CountDownLatch(tasks);

        long start = System.currentTimeMillis();

        for (int i = 0; i < tasks; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(taskDuration);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);

        return System.currentTimeMillis() - start;
    }

    private static long testLongAdder(int threads, long iterations) {
        LongAdder adder = new LongAdder();
        CountDownLatch latch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (long j = 0; j < iterations; j++) {
                        adder.increment();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        long start = System.currentTimeMillis();
        startLatch.countDown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return System.currentTimeMillis() - start;
    }

    private static long testAtomicLong(int threads, long iterations) {
        AtomicLong atomic = new AtomicLong(0);
        CountDownLatch latch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (long j = 0; j < iterations; j++) {
                        atomic.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        long start = System.currentTimeMillis();
        startLatch.countDown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return System.currentTimeMillis() - start;
    }

    private static long testStringBuilder(int threads, int operations) {
        StringBuilder[] builders = new StringBuilder[threads];
        for (int i = 0; i < threads; i++) {
            builders[i] = new StringBuilder();
        }

        CountDownLatch latch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operations; j++) {
                        builders[threadId].append("test");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        long start = System.currentTimeMillis();
        startLatch.countDown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return System.currentTimeMillis() - start;
    }

    private static long testStringBuffer(int threads, int operations) {
        StringBuffer[] buffers = new StringBuffer[threads];
        for (int i = 0; i < threads; i++) {
            buffers[i] = new StringBuffer();
        }

        CountDownLatch latch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operations; j++) {
                        buffers[threadId].append("test");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        long start = System.currentTimeMillis();
        startLatch.countDown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return System.currentTimeMillis() - start;
    }

    private static String formatStats(List<Long> times) {
        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        for (long time : times) {
            sum += time;
            min = Math.min(min, time);
            max = Math.max(max, time);
        }

        double avg = (double) sum / times.size();
        double variance = 0;
        for (long time : times) {
            variance += Math.pow(time - avg, 2);
        }
        double stdDev = Math.sqrt(variance / times.size());

        return String.format("平均=%.0f, 最小=%d, 最大=%d, 标准差=%.0f",
                avg, min, max, stdDev);
    }

    private static String compare(List<Long> times1, List<Long> times2) {
        double avg1 = times1.stream().mapToLong(Long::longValue).average().orElse(0);
        double avg2 = times2.stream().mapToLong(Long::longValue).average().orElse(0);
        double improvement = ((avg2 - avg1) / avg2) * 100;
        return String.format("%.1f%%", improvement);
    }

    /**
     * JMH 使用说明
     */
    public static void jmhGuide() {
        System.out.println("\n=== JMH 使用指南 ===");

        System.out.println("""
        1. 添加 JMH 依赖
           <dependency>
               <groupId>org.openjdk.jmh</groupId>
               <artifactId>jmh-core</artifactId>
               <version>1.36</version>
           </dependency>
           <dependency>
               <groupId>org.openjdk.jmh</groupId>
               <artifactId>jmh-generator-annprocess</artifactId>
               <version>1.36</version>
               <scope>provided</scope>
           </dependency>

        2. 基础示例
           @Benchmark
           @BenchmarkMode(Mode.AverageTime)
           @OutputTimeUnit(TimeUnit.MILLISECONDS)
           public void testMethod() {
               // 被测试的代码
           }

        3. 常用注解
           @Benchmark - 标记基准测试方法
           @BenchmarkMode - 测试模式
           @OutputTimeUnit - 输出时间单位
           @Warmup - 预热配置
           @Measurement - 测量配置
           @Threads - 线程数
           @Fork - Fork 数量

        4. 运行 JMH
           Options opt = new OptionsBuilder()
               .include(ClassName.class.getSimpleName())
               .build();
           new Runner(opt).run();

        5. 为什么使用 JMH
           - 自动处理预热
           - 避免 JIT 优化干扰
           - 准确的统计计算
           - 专业的性能测试框架
        """);
    }

    /**
     * 最佳实践与注意事项
     */
    public static void bestPractices() {
        System.out.println("\n=== 最佳实践与注意事项 ===");

        System.out.println("""
        1. 预热的重要性
           - JVM 需要预热才能达到最佳性能
           - 通常需要几千到几万次迭代
           - JIT 编译需要时间

        2. 多次运行
           - 单次运行结果不可靠
           - 至少运行 3-5 次
           - 取平均值和标准差

        3. 统计指标
           - 平均值 - 总体性能
           - 中位数 - 排除极端值
           - 标准差 - 稳定性
           - 最小值/最大值 - 波动范围

        4. 排除干扰
           - 关闭 GC 日志
           - 控制系统负载
           - 使用独立环境
           - 避免其他进程干扰

        5. 测试环境
           - 使用生产相似的硬件
           - 考虑 JVM 版本差异
           - 注意操作系统差异
           - 考虑物理机 vs 虚拟机

        6. 测试场景
           - 覆盖典型使用场景
           - 考虑不同负载水平
           - 测试边界条件
           - 模拟真实数据

        7. 性能瓶颈分析
           - 使用 profilers 工具
           - 分析 CPU 使用
           - 检查内存访问
           - 查看 GC 情况

        8. 对比要公平
           - 使用相同的测试环境
           - 相同的测试数据
           - 相同的运行时间
           - 相同的预热策略

        9. 记录测试条件
           - 硬件配置
           - JVM 版本和参数
           - 操作系统版本
           - 测试数据规模

        10. 结论要谨慎
            - 微基准测试不等于真实性能
            - 考虑实际应用场景
            - 不要过早优化
            - 关注整体性能而非局部
        """);
    }

    public static void main(String[] args) throws InterruptedException {
        synchronizedVsReentrantLock();
        mapComparison();
        atomicVsSynchronizedCounter();
        blockingQueueComparison();
        threadPoolComparison();
        longAdderVsAtomicLong();
        stringComparison();
        jmhGuide();
        bestPractices();
    }
}
