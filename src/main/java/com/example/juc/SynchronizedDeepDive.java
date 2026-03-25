package com.example.juc;

/**
 * Synchronized关键字深度剖析
 * 学习synchronized的实现原理、锁升级机制和最佳实践
 */
public class SynchronizedDeepDive {

    // 对象锁示例
    private final Object lock = new Object();
    private int counter = 0;

    // 静态锁示例
    private static int staticCounter = 0;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Synchronized关键字深度剖析 ===\n");

        // 1. 同步方法 vs 同步代码块
        System.out.println("1. 同步方法 vs 同步代码块:");
        methodVsBlock();

        // 2. 对象锁 vs 类锁
        System.out.println("\n2. 对象锁 vs 类锁:");
        objectLockVsClassLock();

        // 3. 可重入性演示
        System.out.println("\n3. 可重入性演示:");
        reentrancyDemo();

        // 4. 锁升级机制概念
        System.out.println("\n4. 锁升级机制:");
        lockUpgradeConcept();

        // 5. 锁释放时机
        System.out.println("\n5. 锁释放时机:");
        lockReleaseTiming();

        // 6. 死锁示例及解决
        System.out.println("\n6. 死锁检测与预防:");
        deadlockDemo();

        // 7. synchronized vs ReentrantLock对比
        System.out.println("\n7. synchronized vs ReentrantLock:");
        comparison();
    }

    // 1. 同步方法 vs 同步代码块
    private static void methodVsBlock() throws InterruptedException {
        SynchronizedDeepDive example = new SynchronizedDeepDive();

        // 同步方法
        Thread t1 = new Thread(() -> example.syncMethod(), "Thread-1");
        // 同步代码块
        Thread t2 = new Thread(() -> example.syncBlock(), "Thread-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("  同步方法和同步代码块使用同一把对象锁，互斥执行");
    }

    // 同步方法 - 锁住this对象
    public synchronized void syncMethod() {
        System.out.println("  " + Thread.currentThread().getName() + " 进入同步方法");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("  " + Thread.currentThread().getName() + " 退出同步方法");
    }

    // 同步代码块 - 锁住this对象
    public void syncBlock() {
        synchronized (this) {
            System.out.println("  " + Thread.currentThread().getName() + " 进入同步代码块");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("  " + Thread.currentThread().getName() + " 退出同步代码块");
        }
    }

    // 2. 对象锁 vs 类锁
    private static void objectLockVsClassLock() throws InterruptedException {
        SynchronizedDeepDive obj1 = new SynchronizedDeepDive();
        SynchronizedDeepDive obj2 = new SynchronizedDeepDive();

        // 对象锁 - 不同对象不互斥
        Thread t1 = new Thread(() -> obj1.objectLockMethod(), "Thread-A");
        Thread t2 = new Thread(() -> obj2.objectLockMethod(), "Thread-B");

        System.out.println("  两个不同对象的对象锁 - 可以同时执行");
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println();

        // 类锁 - 所有实例互斥
        Thread t3 = new Thread(() -> obj1.classLockMethod(), "Thread-C");
        Thread t4 = new Thread(() -> obj2.classLockMethod(), "Thread-D");

        System.out.println("  类锁 - 不同对象也互斥执行");
        t3.start();
        t4.start();
        t3.join();
        t4.join();
    }

    // 对象锁方法
    public synchronized void objectLockMethod() {
        System.out.println("  " + Thread.currentThread().getName() + " 获取对象锁");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("  " + Thread.currentThread().getName() + " 释放对象锁");
    }

    // 类锁方法
    public static synchronized void classLockMethod() {
        System.out.println("  " + Thread.currentThread().getName() + " 获取类锁");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("  " + Thread.currentThread().getName() + " 释放类锁");
    }

    // 3. 可重入性演示
    private static void reentrancyDemo() {
        System.out.println("  演示线程可以重复获取它已经持有的锁:");
        new ReentrantTest().start();
    }

    // 可重入测试类
    static class ReentrantTest extends Thread {
        @Override
        public void run() {
            outer();
        }

        // 外层方法
        public void outer() {
            synchronized (this) {
                System.out.println("  外层获取锁");
                inner(); // 重新进入同一把锁
                System.out.println("  外层释放锁");
            }
        }

        // 内层方法
        public void inner() {
            synchronized (this) {
                System.out.println("  内层获取锁（重入）");
            }
        }
    }

    // 4. 锁升级机制概念
    private static void lockUpgradeConcept() {
        System.out.println("  JVM的synchronized锁升级机制:");
        System.out.println("  1. 偏向锁 - 只有一个线程访问时，无锁竞争");
        System.out.println("  2. 轻量级锁 - 有少量线程竞争，使用CAS");
        System.out.println("  3. 重量级锁 - 竞争激烈，使用OS互斥量");
        System.out.println();
        System.out.println("  升级过程: 偏向锁 -> 轻量级锁 -> 重量级锁");
        System.out.println("  注意: 锁只能升级，不能降级（JVM实现细节）");
        System.out.println("  优化: -XX:+UseBiasedLocking（JDK 15后默认关闭）");
    }

    // 5. 锁释放时机
    private static void lockReleaseTiming() {
        System.out.println("  锁自动释放的时机:");
        System.out.println("  1. 代码块/方法正常执行完毕");
        System.out.println("  2. 代码块/方法抛出异常（仍然会释放锁）");
        System.out.println("  3. 遇到break/continue/return");
        System.out.println();
        System.out.println("  注意: 不会因为Thread.sleep()或wait()而释放锁");
        System.out.println("  wait()会释放锁，但需要配合synchronized使用");
    }

    // 6. 死锁示例及解决
    private static void deadlockDemo() throws InterruptedException {
        final Object lock1 = new Object();
        final Object lock2 = new Object();

        System.out.println("  死锁示例: 两个线程以相反顺序获取锁");

        // 创建死锁
        Thread t1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("  线程1获取lock1");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("  线程1等待lock2...");
                synchronized (lock2) {
                    System.out.println("  线程1获取lock2");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("  线程2获取lock2");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("  线程2等待lock1...");
                synchronized (lock1) {
                    System.out.println("  线程2获取lock1");
                }
            }
        });

        t1.start();
        t2.start();

        Thread.sleep(1000);

        System.out.println("  检测到死锁，程序将无法继续...");
        System.out.println();
        System.out.println("  解决方案:");
        System.out.println("  1. 固定锁的获取顺序");
        System.out.println("  2. 使用tryLock设置超时");
        System.out.println("  3. 使用JMX工具检测死锁: jconsole, jvisualvm");

        t1.interrupt();
        t2.interrupt();
    }

    // 7. synchronized vs ReentrantLock对比
    private static void comparison() {
        System.out.println("  | 特性                | synchronized | ReentrantLock |");
        System.out.println("  |---------------------|-------------|---------------|");
        System.out.println("  | 使用难度            | 简单        | 较复杂        |");
        System.out.println("  | 锁释放              | 自动        | 手动          |");
        System.out.println("  | 可中断              | 否          | 是(lockInterruptibly)|");
        System.out.println("  | 超时获取            | 否          | 是(tryLock)   |");
        System.out.println("  | 公平锁              | 否          | 可配置        |");
        System.out.println("  | 条件变量            | wait/notify | Condition     |");
        System.out.println("  | 性能(JDK 6+)        | 优化较好    | 相当          |");
        System.out.println("  | 锁状态信息          | 无          | 可查询        |");
        System.out.println();
        System.out.println("  选择建议:");
        System.out.println("  - 一般情况: 优先使用synchronized");
        System.out.println("  - 需要中断/超时: 使用ReentrantLock");
        System.out.println("  - 需要多个Condition: 使用ReentrantLock");
        System.out.println("  - 需要公平锁: 使用ReentrantLock(true)");
    }
}
