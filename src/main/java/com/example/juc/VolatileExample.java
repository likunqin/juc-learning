package com.example.juc;

/**
 * Volatile关键字学习示例
 * 演示volatile的可见性、有序性和不保证原子性的特性
 */
public class VolatileExample {

    // 1. 可见性演示
    private static volatile boolean running = true;

    // 2. 非原子性演示
    private static volatile int counter = 0;

    // 3. 指令重排序演示
    private static int x = 0, y = 0;
    private static volatile int a = 0, b = 0;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Volatile关键字学习示例 ===\n");

        // 1. 可见性演示
        System.out.println("1. 可见性演示:");
        visibilityDemo();

        // 2. 非原子性演示
        System.out.println("\n2. 非原子性演示 (volatile不能保证原子性):");
        atomicityDemo();

        // 3. 指令重排序演示
        System.out.println("\n3. 指令重排序 (volatile禁止重排序):");
        reorderingDemo();

        // 4. volatile与synchronized对比
        System.out.println("\n4. volatile与synchronized对比:");
        comparison();

        // 5. 单例模式的double-check锁
        System.out.println("\n5. 单例模式double-check锁:");
        singletonDemo();
    }

    // 1. 可见性演示
    private static void visibilityDemo() throws InterruptedException {
        Thread worker = new Thread(() -> {
            System.out.println("  工作线程启动，running=" + running);
            while (running) {
                // 空循环，如果没有volatile，可能看不到running的变化
            }
            System.out.println("  工作线程检测到running=false，退出循环");
        });

        worker.start();
        Thread.sleep(100); // 让工作线程运行一会儿

        System.out.println("  主线程设置running=false");
        running = false; // volatile保证这个修改对其他线程可见

        worker.join();
    }

    // 2. 非原子性演示
    private static void atomicityDemo() throws InterruptedException {
        counter = 0;
        final int THREADS = 10;
        final int INCREMENTS = 1000;

        Thread[] threads = new Thread[THREADS];

        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < INCREMENTS; j++) {
                    counter++; // 不是原子操作：读取、修改、写入
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("  期望值: " + (THREADS * INCREMENTS));
        System.out.println("  实际值: " + counter);
        System.out.println("  结论: volatile不能保证复合操作的原子性");
        System.out.println("  解决方案: 使用AtomicInteger或synchronized");
    }

    // 3. 指令重排序演示
    private static void reorderingDemo() throws InterruptedException {
        // 演示volatile禁止指令重排序的效果
        int reorderCount = 0;
        int iterations = 100000;

        for (int i = 0; i < iterations; i++) {
            x = 0; y = 0; a = 0; b = 0;

            Thread one = new Thread(() -> {
                a = 1;
                x = b;
            });

            Thread two = new Thread(() -> {
                b = 1;
                y = a;
            });

            one.start();
            two.start();
            one.join();
            two.join();

            // 如果发生重排序，可能出现 (1,1)
            if (x == 0 && y == 0) {
                reorderCount++;
            }
        }

        System.out.println("  测试次数: " + iterations);
        System.out.println("  发生(0,0)情况的次数: " + reorderCount);
        System.out.println("  结论: volatile变量可以禁止指令重排序");
    }

    // 4. volatile与synchronized对比
    private static void comparison() {
        System.out.println("  volatile:");
        System.out.println("    ✓ 保证可见性");
        System.out.println("    ✓ 禁止指令重排序");
        System.out.println("    ✗ 不保证原子性");
        System.out.println("    ✓ 轻量级，不会阻塞线程");
        System.out.println("    ✓ 适用于状态标志、单例");

        System.out.println("\n  synchronized:");
        System.out.println("    ✓ 保证可见性");
        System.out.println("    ✓ 保证原子性");
        System.out.println("    ✓ 支持锁升级（偏向锁->轻量级锁->重量级锁）");
        System.out.println("    ✓ 可重入");
        System.out.println("    ✗ 重量级，可能阻塞线程");
        System.out.println("    ✓ 适用于复合操作、临界区保护");
    }

    // 5. 单例模式double-check锁
    private static void singletonDemo() {
        // 创建多个线程测试单例
        Thread[] threads = new Thread[10];

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                Singleton instance = Singleton.getInstance();
                System.out.println("  " + Thread.currentThread().getName() + " 获取实例: " + instance);
            });
        }

        for (Thread t : threads) t.start();

        try {
            for (Thread t : threads) t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 单例模式 - double-check锁
    static class Singleton {
        // volatile关键字是必须的，防止指令重排序
        private static volatile Singleton instance;

        private Singleton() {
            System.out.println("    Singleton实例被创建");
        }

        public static Singleton getInstance() {
            // 第一次检查，避免不必要的同步
            if (instance == null) {
                synchronized (Singleton.class) {
                    // 第二次检查，确保只有一个实例被创建
                    if (instance == null) {
                        instance = new Singleton();
                        // new Singleton() 实际上是三步操作：
                        // 1. 分配内存
                        // 2. 初始化对象
                        // 3. 将instance指向分配的内存
                        // 如果没有volatile，可能发生重排序为1->3->2，
                        // 导致其他线程获取到未初始化的实例
                    }
                }
            }
            return instance;
        }
    }
}
