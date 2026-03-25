package com.example.juc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java 内存模型示例
 * <p>
 * 演示 Java 内存模型（JMM）的核心概念和规则
 * </p>
 *
 * <h3>JMM 核心概念：</h3>
 * <ul>
 *   <li>原子性 - 操作要么全部执行，要么都不执行</li>
 *   <li>可见性 - 一个线程的修改对其他线程可见</li>
 *   <li>有序性 - 指令执行的顺序</li>
 * </ul>
 *
 * <h3>Happens-Before 规则：</h3>
 * <ul>
 *   <li>程序次序规则 - 单线程内按代码顺序</li>
 *   <li>锁规则 - unlock 先于 lock</li>
 *   <li>volatile 规则 - 写先于读</li>
 *   <li>传递规则 - A 先于 B，B 先于 C，则 A 先于 C</li>
 *   <li>线程启动 - Thread.start() 先于线程动作</li>
 *   <li>线程终止 - 线程终止先于 Thread.join()</li>
 *   <li>中断 - Thread.interrupt() 先于中断检测</li>
 *   <li>对象终结 - 构造函数先于 finalize()</li>
 *   <li>传递性 - 结合以上规则</li>
 * </ul>
 */
public class MemoryModelExample {

    /**
     * 场景1：内存可见性问题
     * <p>
     * 演示缺少同步时的可见性问题
     * </p>
     */
    public static void visibilityProblem() throws InterruptedException {
        System.out.println("=== 场景1：内存可见性问题 ===");

        class SharedData {
            public boolean flag = false; // 没有 volatile
            public int counter = 0;
        }

        SharedData data = new SharedData();

        // 修改线程
        Thread modifier = new Thread(() -> {
            data.counter = 100;
            data.flag = true;
            System.out.println("修改线程: flag = " + data.flag + ", counter = " + data.counter);
        });

        // 读取线程
        Thread reader = new Thread(() -> {
            while (!data.flag) {
                // 空转等待
            }
            System.out.println("读取线程: flag = " + data.flag + ", counter = " + data.counter);
        });

        reader.start();
        Thread.sleep(100); // 确保读取线程先启动
        modifier.start();

        // 等待足够时间（可能永远不会看到修改）
        reader.join(2000);

        if (reader.isAlive()) {
            System.out.println("读取线程仍在等待（可见性问题）");
            reader.interrupt();
        }
    }

    /**
     * 场景2：volatile 解决可见性问题
     * <p>
     * 使用 volatile 保证变量的可见性
     * </p>
     */
    public static void volatileVisibility() throws InterruptedException {
        System.out.println("\n=== 场景2：volatile 保证可见性 ===");

        class SharedData {
            public volatile boolean flag = false;
            public int counter = 0;
        }

        SharedData data = new SharedData();

        Thread reader = new Thread(() -> {
            while (!data.flag) {
                // 空转等待
            }
            System.out.println("读取线程: flag = " + data.flag + ", counter = " + data.counter);
        });

        Thread modifier = new Thread(() -> {
            data.counter = 100;
            data.flag = true;
            System.out.println("修改线程: flag = " + data.flag + ", counter = " + data.counter);
        });

        reader.start();
        Thread.sleep(100);
        modifier.start();

        reader.join(2000);
        System.out.println("volatile 保证了可见性，读取线程正常退出");
    }

    /**
     * 场景3：指令重排序演示
     * <p>
     * 演示在没有同步时可能发生的指令重排序
     * </p>
     */
    public static void instructionReordering() throws InterruptedException {
        System.out.println("\n=== 场景3：指令重排序 ===");

        class ReorderingExample {
            private int x = 0;
            private int y = 0;
            private int a = 0;
            private int b = 0;

            public void writer() {
                x = 1; // 语句1
                y = 2; // 语句2
            }

            public void reader() {
                a = y; // 语句3
                b = x; // 语句4
            }
        }

        ReorderingExample example = new ReorderingExample();
        AtomicInteger reorderingCount = new AtomicInteger(0);
        int iterations = 10000;

        for (int i = 0; i < iterations; i++) {
            example.x = 0;
            example.y = 0;
            example.a = 0;
            example.b = 0;

            Thread t1 = new Thread(example::writer);
            Thread t2 = new Thread(example::reader);

            t1.start();
            t2.start();

            t1.join();
            t2.join();

            // 检查是否发生了重排序
            // 如果 a=0 && b=0，说明发生了重排序（y 被赋值后又被读取时 x 还未赋值）
            if (example.a == 0 && example.b == 1) {
                // 这不是重排序，是并发执行的结果
            } else if (example.a == 2 && example.b == 0) {
                // 这可能是指令重排序
                reorderingCount.incrementAndGet();
            }
        }

        System.out.println("在 " + iterations + " 次迭代中");
        System.out.println("可能的重排序次数: " + reorderingCount.get());
        System.out.println("（注意：可能需要更多迭代才能观察到重排序）");
    }

    /**
     * 场景4：volatile 禁止指令重排序
     * <p>
     * 演示 volatile 如何禁止特定类型的指令重排序
     * </p>
     */
    public static void volatileOrdering() throws InterruptedException {
        System.out.println("\n=== 场景4：volatile 禁止重排序 ===");

        class Singleton {
            private static volatile Singleton instance;
            public int value;

            private Singleton() {
                this.value = 100;
            }

            public static Singleton getInstance() {
                if (instance == null) { // 第一次检查
                    synchronized (Singleton.class) {
                        if (instance == null) { // 第二次检查
                            instance = new Singleton();
                            // volatile 禁止了以下重排序：
                            // 1. 分配内存
                            // 2. 初始化对象
                            // 3. 将引用指向内存
                            // 没有 volatile，可能按 1->3->2 顺序执行
                        }
                    }
                }
                return instance;
            }
        }

        // 创建多个线程同时获取单例
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger nullCount = new AtomicInteger(0);
        AtomicInteger zeroCount = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                Singleton instance = Singleton.getInstance();
                if (instance == null) {
                    nullCount.incrementAndGet();
                } else if (instance.value == 0) {
                    // 如果 value 为 0，说明看到了半初始化对象（重排序）
                    zeroCount.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        System.out.println("null 实例数: " + nullCount.get());
        System.out.println("半初始化实例数（value=0）: " + zeroCount.get());
        System.out.println("volatile 保证不会看到半初始化对象");
    }

    /**
     * 场景5：synchronized 的 Happens-Before
     * <p>
     * 演示 synchronized 保证的可见性和有序性
     * </p>
     */
    public static void synchronizedHappensBefore() throws InterruptedException {
        System.out.println("\n=== 场景5：synchronized Happens-Before ===");

        class SynchronizedData {
            private int x = 0;
            private int y = 0;
            private final Object lock = new Object();

            public void write() {
                synchronized (lock) {
                    x = 1;
                    y = 2;
                }
            }

            public void read() {
                int a, b;
                synchronized (lock) {
                    a = x;
                    b = y;
                }
                System.out.println("读取: a=" + a + ", b=" + b);
            }
        }

        SynchronizedData data = new SynchronizedData();

        Thread writer = new Thread(data::write);
        Thread reader = new Thread(data::read);

        writer.start();
        reader.start();

        writer.join();
        reader.join();
        System.out.println("synchronized 保证 write() 先于 read()");
    }

    /**
     * 场景6：Thread.start() 和 join() 的 Happens-Before
     * <p>
     * 演示线程启动和终止的 Happens-Before 规则
     * </p>
     */
    public static void threadStartAndJoin() throws InterruptedException {
        System.out.println("\n=== 场景6：Thread.start() 和 join() ===");

        final AtomicInteger value = new AtomicInteger(0);

        // Thread.start() happens-before 线程中的动作
        Thread thread = new Thread(() -> {
            // 线程启动前主线程的操作对线程可见
            System.out.println("线程读取 value: " + value.get());
            value.set(100);
            System.out.println("线程设置 value = 100");
        });

        value.set(10);
        thread.start(); // happens-before 线程的动作

        // Thread.join() happens-before join() 返回
        thread.join(); // happens-after 线程终止

        System.out.println("join() 后主线程读取 value: " + value.get());
        System.out.println("value = 100（线程的修改对主线程可见）");
    }

    /**
     * 场景7：原子性 vs 可见性
     * <p>
     * 演示原子操作不保证可见性，可见性不保证原子性
     * </p>
     */
    public static void atomicityVsVisibility() throws InterruptedException {
        System.out.println("\n=== 场景7：原子性 vs 可见性 ===");

        class Counter {
            // 可见但不原子
            public volatile int volatileCounter = 0;

            // 原子但不保证特定语义的可见性顺序
            public final AtomicInteger atomicCounter = new AtomicInteger(0);

            // 既可见又原子
            public int synchronizedCounter = 0;
            private final Object lock = new Object();
        }

        Counter counter = new Counter();
        int threads = 10;
        int increments = 10000;
        CountDownLatch latch = new CountDownLatch(threads);

        // 多线程并发增加计数器
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                for (int j = 0; j < increments; j++) {
                    counter.volatileCounter++; // 读-改-写，不原子
                    counter.atomicCounter.incrementAndGet(); // 原子操作
                    synchronized (counter.lock) {
                        counter.synchronizedCounter++; // 同步块，既原子又可见
                    }
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        System.out.println("预期值: " + (threads * increments));
        System.out.println("volatile 计数器（不原子）: " + counter.volatileCounter);
        System.out.println("AtomicInteger（原子）: " + counter.atomicCounter.get());
        System.out.println("synchronized 计数器（原子+可见）: " + counter.synchronizedCounter);
        System.out.println("\n结论:");
        System.out.println("- volatile 保证可见性，但不保证复合操作的原子性");
        System.out.println("- AtomicInteger 保证原子性");
        System.out.println("- synchronized 既保证原子性又保证可见性");
    }

    /**
     * 场景8：final 字段的内存语义
     * <p>
     * 演示 final 字段在对象发布时的内存语义
     * </p>
     */
    public static void finalFieldSemantics() throws InterruptedException {
        System.out.println("\n=== 场景8：final 字段语义 ===");

        class FinalExample {
            private final int x;
            private final int y;

            public FinalExample() {
                x = 1;
                y = 2;
                // final 字段在构造函数完成后，对其他线程可见
            }

            public int getX() { return x; }
            public int getY() { return y; }
        }

        FinalExample obj = new FinalExample();

        Thread reader = new Thread(() -> {
            // 保证能看到 final 字段的正确值
            System.out.println("读取 final 字段: x=" + obj.getX() + ", y=" + obj.getY());
        });

        reader.start();
        reader.join();
        System.out.println("final 字段保证在对象发布后可见");
    }

    /**
     * 场景9：传递性规则
     * <p>
     * 演示 Happens-Before 的传递性
     * </p>
     */
    public static void transitivity() throws InterruptedException {
        System.out.println("\n=== 场景9：传递性规则 ===");

        class TransitivityExample {
            private int x = 0;
            private volatile boolean flag = false;

            public void write() {
                x = 42;          // (1)
                flag = true;     // (2)
                // (1) happens-before (2) 程序次序规则
            }

            public void read() {
                if (flag) {      // (3)
                    // (2) happens-before (3) volatile 规则
                    // (1) happens-before (3) 传递性
                    int local = x; // (4)
                    System.out.println("读取 x: " + local);
                    // 保证能读到 42，而不是 0
                }
            }
        }

        TransitivityExample example = new TransitivityExample();

        Thread writer = new Thread(example::write);
        Thread reader = new Thread(example::read);

        writer.start();
        reader.start();

        writer.join();
        reader.join();
        System.out.println("通过传递性保证 x=42 的可见性");
    }

    /**
     * 最佳实践与注意事项
     */
    public static void bestPractices() {
        System.out.println("\n=== 最佳实践与注意事项 ===");

        System.out.println("""
        1. 理解 Happens-Before 规则
           - 这些规则是编写正确并发程序的基础
           - 不需要死记硬背，但需要理解
           - 关键是知道哪些操作有保证

        2. volatile 的正确使用
           - 适用于保证可见性
           - 禁止指令重排序
           - 不保证复合操作的原子性

        3. synchronized 的使用
           - 保证原子性和可见性
           - 进入/退出内存屏障
           - 适用范围更广

        4. final 字段
           - 保证安全发布
           - 对象构造完成后 final 字段立即可见
           - 用于不可变对象

        5. 共享变量保护
           - 访问共享变量需要同步
           - 无论是读还是写
           - 使用 volatile 或 synchronized

        6. 避免数据竞争
           - 数据竞争会导致不确定的结果
           - 没有同步的读写
           - 使用工具检查

        7. 理解内存屏障
           - LoadLoad / LoadStore
           - StoreStore / StoreLoad
           - volatile 和 synchronized 会插入屏障

        8. 性能考虑
           - volatile 比 synchronized 轻量
           - 但不能解决所有问题
           - 根据场景选择

        9. 原子类
           - 提供原子操作
           - 使用 CAS 实现
           - 不涉及内存阻塞

        10. 逃逸分析
            - 局部变量不会逃逸时，不需要同步
            - JIT 编译器可以优化
            - 但要保证逻辑正确
        """);
    }

    public static void main(String[] args) throws InterruptedException {
        visibilityProblem();
        volatileVisibility();
        instructionReordering();
        volatileOrdering();
        synchronizedHappensBefore();
        threadStartAndJoin();
        atomicityVsVisibility();
        finalFieldSemantics();
        transitivity();
        bestPractices();
    }
}
