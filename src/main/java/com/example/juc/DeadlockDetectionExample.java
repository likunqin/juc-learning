package com.example.juc;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * 死锁检测与预防示例
 * <p>
 * 演示死锁的产生、检测和预防方法
 * </p>
 *
 * <h3>死锁的四个必要条件：</h3>
 * <ul>
 *   <li>互斥条件 - 资源不能被共享</li>
 *   <li>持有并等待 - 持有资源同时等待其他资源</li>
 *   <li>不可剥夺 - 资源不能被强制剥夺</li>
 *   <li>循环等待 - 形成等待环路</li>
 * </ul>
 *
 * <h3>死锁预防策略：</h3>
 * <ul>
 *   <li>破坏循环等待 - 统一锁获取顺序</li>
 *   <li>破坏持有并等待 - 一次性获取所有锁</li>
 *   <li>破坏不可剥夺 - 使用 tryLock 带超时</li>
 *   <li>破坏互斥 - 使用读写锁等</li>
 * </ul>
 */
public class DeadlockDetectionExample {

    /**
     * 场景1：简单的死锁演示
     * <p>
     * 两个线程以不同的顺序获取两个锁，导致死锁
     * </p>
     */
    public static void simpleDeadlock() throws InterruptedException {
        System.out.println("=== 场景1：简单死锁演示 ===");

        final Object lock1 = new Object();
        final Object lock2 = new Object();

        Thread t1 = new Thread(() -> {
            System.out.println("线程1: 尝试获取 lock1");
            synchronized (lock1) {
                System.out.println("线程1: 获取到 lock1");
                try {
                    Thread.sleep(100);
                    System.out.println("线程1: 尝试获取 lock2");
                    synchronized (lock2) {
                        System.out.println("线程1: 获取到 lock2");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            System.out.println("线程2: 尝试获取 lock2");
            synchronized (lock2) {
                System.out.println("线程2: 获取到 lock2");
                try {
                    Thread.sleep(100);
                    System.out.println("线程2: 尝试获取 lock1");
                    synchronized (lock1) {
                        System.out.println("线程2: 获取到 lock1");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Thread-2");

        t1.start();
        t2.start();

        // 等待看是否死锁
        Thread.sleep(2000);

        System.out.println("\n检测死锁...");
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();

        if (deadlockedThreads != null) {
            System.out.println("检测到死锁！涉及线程数: " + deadlockedThreads.length);
            for (long threadId : deadlockedThreads) {
                ThreadInfo threadInfo = threadMXBean.getThreadInfo(threadId);
                System.out.println("  " + threadInfo.getThreadName() +
                        " 在等待锁: " + Arrays.toString(threadInfo.getLockedMonitors()) +
                        " 持有锁: " + Arrays.toString(threadInfo.getLockedSynchronizers()));
            }

            // 强制中断死锁线程（仅用于演示，生产环境应修复死锁）
            t1.interrupt();
            t2.interrupt();
        } else {
            System.out.println("没有检测到死锁");
        }

        t1.join(1000);
        t2.join(1000);
    }

    /**
     * 场景2：使用 ReentrantLock 的 tryLock 预防死锁
     * <p>
     * 使用带超时的 tryLock 可以在获取失败时释放已持有的锁
     * </p>
     */
    public static void preventDeadlockWithTryLock() throws InterruptedException {
        System.out.println("\n=== 场景2：使用 tryLock 预防死锁 ===");

        Lock lock1 = new ReentrantLock();
        Lock lock2 = new ReentrantLock();

        Runnable task = (int id) -> {
            System.out.println("线程 " + id + ": 尝试获取两个锁");
            long startTime = System.currentTimeMillis();

            while (true) {
                if (lock1.tryLock(100, TimeUnit.MILLISECONDS)) {
                    try {
                        System.out.println("线程 " + id + ": 获取到 lock1");
                        if (lock2.tryLock(100, TimeUnit.MILLISECONDS)) {
                            try {
                                System.out.println("线程 " + id + ": 获取到 lock2");
                                System.out.println("线程 " + id + ": 执行工作");
                                return; // 成功获取两个锁，退出
                            } finally {
                                lock2.unlock();
                            }
                        } else {
                            System.out.println("线程 " + id + ": lock2 获取失败，重试");
                        }
                    } finally {
                        lock1.unlock();
                    }
                }

                // 检查是否超时
                if (System.currentTimeMillis() - startTime > 5000) {
                    System.out.println("线程 " + id + ": 获取锁超时，放弃");
                    return;
                }
                // 随机退避
                Thread.sleep(ThreadLocalRandom.current().nextInt(50, 200));
            }
        };

        Thread t1 = new Thread(() -> task.run(1), "Thread-1");
        Thread t2 = new Thread(() -> task.run(2), "Thread-2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();
        System.out.println("两个线程都成功完成，没有死锁");
    }

    /**
     * 场景3：统一锁顺序预防死锁
     * <p>
     * 所有线程按相同的顺序获取锁，避免循环等待
     * </p>
     */
    public static void preventDeadlockWithLockOrdering() throws InterruptedException {
        System.out.println("\n=== 场景3：统一锁顺序 ===");

        final Object lock1 = new Object();
        final Object lock2 = new Object();

        // 定义锁的顺序，确保所有线程按相同顺序获取
        Runnable task = (int id, boolean needsBoth) -> {
            // 总是先获取 lock1，再获取 lock2
            if (needsBoth) {
                synchronized (lock1) {
                    System.out.println("线程 " + id + ": 获取到 lock1");
                    try {
                        Thread.sleep(100);
                        synchronized (lock2) {
                            System.out.println("线程 " + id + ": 获取到 lock2");
                            System.out.println("线程 " + id + ": 执行工作");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                // 只需要一个锁的情况
                synchronized (lock1) {
                    System.out.println("线程 " + id + ": 只使用 lock1");
                }
            }
        };

        Thread t1 = new Thread(() -> task.run(1, true), "Thread-1");
        Thread t2 = new Thread(() -> task.run(2, true), "Thread-2");
        Thread t3 = new Thread(() -> task.run(3, false), "Thread-3");

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();
        System.out.println("所有线程都成功完成");
    }

    /**
     * 场景4：银行家算法模拟
     * <p>
     * 演示资源分配前的安全检查，避免死锁
     * </p>
     */
    public static void bankersAlgorithm() throws InterruptedException {
        System.out.println("\n=== 场景4：银行家算法 ===");

        class ResourceManager {
            private final int totalResources;
            private int available;
            private final Map<String, Integer> allocated = new ConcurrentHashMap<>();
            private final Map<String, Integer> maxRequest = new ConcurrentHashMap<>();

            public ResourceManager(int total) {
                this.totalResources = total;
                this.available = total;
            }

            public boolean register(String process, int max) {
                if (max > totalResources) {
                    return false;
                }
                maxRequest.put(process, max);
                allocated.put(process, 0);
                return true;
            }

            public synchronized boolean request(String process, int request) {
                System.out.println(process + " 请求 " + request + " 个资源，可用: " + available);

                // 检查请求是否超过最大需求
                int currentAllocated = allocated.get(process);
                int maxNeed = maxRequest.get(process);
                if (request > maxNeed - currentAllocated) {
                    System.out.println(process + " 请求超过最大需求，拒绝");
                    return false;
                }

                // 检查是否有足够资源
                if (request > available) {
                    System.out.println(process + " 资源不足，等待");
                    return false;
                }

                // 尝试分配，检查是否安全
                available -= request;
                allocated.put(process, currentAllocated + request);

                if (isSafeState()) {
                    System.out.println(process + " 分配成功，可用: " + available);
                    return true;
                } else {
                    // 回滚分配
                    available += request;
                    allocated.put(process, currentAllocated);
                    System.out.println(process + " 分配会导致不安全状态，拒绝");
                    return false;
                }
            }

            public synchronized void release(String process, int count) {
                int current = allocated.get(process);
                if (count > current) {
                    throw new IllegalArgumentException("释放数量超过持有数量");
                }
                allocated.put(process, current - count);
                available += count;
                System.out.println(process + " 释放 " + count + " 个资源，可用: " + available);
            }

            private boolean isSafeState() {
                // 简化版安全检查：确保至少有一个进程能完成
                for (Map.Entry<String, Integer> entry : allocated.entrySet()) {
                    String process = entry.getKey();
                    int allocatedCount = entry.getValue();
                    int max = maxRequest.get(process);
                    int need = max - allocatedCount;

                    if (need <= available) {
                        return true;
                    }
                }
                return false;
            }
        }

        ResourceManager manager = new ResourceManager(5);

        // 注册进程
        manager.register("P1", 3);
        manager.register("P2", 4);
        manager.register("P3", 2);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        CountDownLatch done = new CountDownLatch(3);

        // 进程P1
        executor.submit(() -> {
            try {
                while (!manager.request("P1", 2)) {
                    Thread.sleep(500);
                }
                Thread.sleep(1000);
                manager.release("P1", 2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });

        // 进程P2
        executor.submit(() -> {
            try {
                while (!manager.request("P2", 3)) {
                    Thread.sleep(500);
                }
                Thread.sleep(1000);
                manager.release("P2", 3);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });

        // 进程P3
        executor.submit(() -> {
            try {
                while (!manager.request("P3", 1)) {
                    Thread.sleep(500);
                }
                Thread.sleep(1000);
                manager.release("P3", 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        });

        done.await();
        executor.shutdown();
        System.out.println("所有进程完成，没有死锁");
    }

    /**
     * 场景5：超时获取锁
     * <p>
     * 使用 lockInterruptibly() 和 tryLock() 处理可能的死锁
     * </p>
     */
    public static void lockWithTimeout() throws InterruptedException {
        System.out.println("\n=== 场景5：带超时的锁获取 ===");

        Lock lock1 = new ReentrantLock();
        Lock lock2 = new ReentrantLock();

        Thread t1 = new Thread(() -> {
            try {
                lock1.lockInterruptibly();
                System.out.println("线程1: 获取 lock1");
                Thread.sleep(2000);
                System.out.println("线程1: 尝试获取 lock2");
                lock2.lockInterruptibly();
                System.out.println("线程1: 获取 lock2");
            } catch (InterruptedException e) {
                System.out.println("线程1: 被中断，释放锁");
                if (lock1.tryLock()) {
                    lock1.unlock();
                }
                Thread.currentThread().interrupt();
            } finally {
                if (lock1.tryLock()) {
                    lock1.unlock();
                }
                if (lock2.tryLock()) {
                    lock2.unlock();
                }
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                // 尝试获取 lock2，带超时
                if (lock2.tryLock(1, TimeUnit.SECONDS)) {
                    System.out.println("线程2: 获取 lock2");
                    try {
                        // 尝试获取 lock1，带超时
                        if (lock1.tryLock(1, TimeUnit.SECONDS)) {
                            System.out.println("线程2: 获取 lock1");
                            lock1.unlock();
                        } else {
                            System.out.println("线程2: lock1 获取超时");
                        }
                    } finally {
                        lock2.unlock();
                    }
                } else {
                    System.out.println("线程2: lock2 获取超时");
                }
            } catch (InterruptedException e) {
                System.out.println("线程2: 被中断");
                Thread.currentThread().interrupt();
            }
        });

        t1.start();
        t2.start();

        Thread.sleep(3000);
        if (t1.isAlive() || t2.isAlive()) {
            System.out.println("线程仍在运行，中断它们");
            t1.interrupt();
            t2.interrupt();
        }

        t1.join();
        t2.join();
    }

    /**
     * 场景6：活锁演示
     * <p>
     * 两个线程互相退让，但始终无法推进
     * </p>
     */
    public static void livelock() throws InterruptedException {
        System.out.println("\n=== 场景6：活锁 ===");

        class LivelockExample {
            private boolean resource1InUse = false;
            private boolean resource2InUse = false;

            public void useResource1() throws InterruptedException {
                while (resource1InUse) {
                    System.out.println("等待 resource1 可用...");
                    Thread.sleep(500);
                }
                resource1InUse = true;
            }

            public void releaseResource1() {
                resource1InUse = false;
            }

            public void useResource2() throws InterruptedException {
                while (resource2InUse) {
                    System.out.println("等待 resource2 可用...");
                    Thread.sleep(500);
                }
                resource2InUse = true;
            }

            public void releaseResource2() {
                resource2InUse = false;
            }
        }

        LivelockExample example = new LivelockExample();

        Thread t1 = new Thread(() -> {
            try {
                int attempts = 0;
                while (attempts < 5) {
                    example.useResource1();
                    try {
                        Thread.sleep(100);
                        example.useResource2();
                        System.out.println("线程1 成功获取两个资源");
                        example.releaseResource1();
                        example.releaseResource2();
                        break;
                    } catch (Exception e) {
                        example.releaseResource1();
                        System.out.println("线程1 获取 resource2 失败，释放 resource1");
                        attempts++;
                    }
                }
                if (attempts >= 5) {
                    System.out.println("线程1 放弃");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                int attempts = 0;
                while (attempts < 5) {
                    example.useResource2();
                    try {
                        Thread.sleep(100);
                        example.useResource1();
                        System.out.println("线程2 成功获取两个资源");
                        example.releaseResource1();
                        example.releaseResource2();
                        break;
                    } catch (Exception e) {
                        example.releaseResource2();
                        System.out.println("线程2 获取 resource1 失败，释放 resource2");
                        attempts++;
                    }
                }
                if (attempts >= 5) {
                    System.out.println("线程2 放弃");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }

    /**
     * 场景7：锁顺序哈希算法
     * <p>
     * 使用对象哈希值确定锁顺序，避免死锁
     * </p>
     */
    public static void lockOrderingWithHash() throws InterruptedException {
        System.out.println("\n=== 场景7：基于哈希的锁顺序 ===");

        class OrderedLockManager {
            private final Map<Object, Lock> locks = new ConcurrentHashMap<>();

            public void runWithLocks(Object a, Object b, Runnable action) {
                // 按哈希值确定顺序
                int hashA = System.identityHashCode(a);
                int hashB = System.identityHashCode(b);

                Object first = hashA < hashB ? a : b;
                Object second = hashA < hashB ? b : a;

                synchronized (first) {
                    synchronized (second) {
                        action.run();
                    }
                }
            }
        }

        OrderedLockManager manager = new OrderedLockManager();

        Object obj1 = new Object();
        Object obj2 = new Object();

        Thread t1 = new Thread(() -> {
            manager.runWithLocks(obj1, obj2, () -> {
                System.out.println("线程1 在两个对象上执行操作");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });

        Thread t2 = new Thread(() -> {
            manager.runWithLocks(obj2, obj1, () -> {
                System.out.println("线程2 在两个对象上执行操作");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();
        System.out.println("两个线程都成功完成，锁顺序自动确定");
    }

    /**
     * 最佳实践与注意事项
     */
    public static void bestPractices() {
        System.out.println("\n=== 最佳实践与注意事项 ===");

        System.out.println("""
        1. 统一锁顺序
           - 所有线程按相同顺序获取锁
           - 可以基于对象的哈希值或预先定义的顺序
           - 避免循环等待

        2. 使用 tryLock 超时
           - 不要使用无限期阻塞的 lock()
           - 使用 tryLock(timeout) 获取锁
           - 超时后释放已持有的锁

        3. 使用 lockInterruptibly
           - 支持中断的锁获取
           - 被中断时可以清理资源
           - 比不可中断的方式更安全

        4. 减少锁的持有时间
           - 只在必要时持有锁
           - 避免在锁内执行耗时操作
           - 减小死锁窗口

        5. 使用更高级的同步工具
           - 优先使用 BlockingQueue
           - 使用并发集合而非手动同步
           - 使用 CountDownLatch/CyclicBarrier 协调

        6. 死锁检测
           - 使用 ThreadMXBean.findDeadlockedThreads()
           - 定期检查线程状态
           - 监控锁等待时间

        7. 避免活锁
           - 退避时加入随机性
           - 不要简单地无条件重试
           - 设置重试上限

        8. 银行家算法
           - 资源分配前进行安全检查
           - 预先声明最大需求
           - 适用于资源有限场景

        9. 代码审查
           - 重点审查多锁场景
           - 检查锁的获取顺序
           - 识别潜在的死锁模式

        10. 监控和告警
            - 监控线程阻塞情况
            - 设置死锁检测和告警
            - 及时发现和处理死锁
        """);
    }

    public static void main(String[] args) throws InterruptedException {
        simpleDeadlock();
        preventDeadlockWithTryLock();
        preventDeadlockWithLockOrdering();
        bankersAlgorithm();
        lockWithTimeout();
        livelock();
        lockOrderingWithHash();
        bestPractices();
    }
}
