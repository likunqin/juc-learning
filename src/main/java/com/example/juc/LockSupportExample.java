package com.example.juc;

import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * LockSupport 工具类示例
 * <p>
 * LockSupport 是 JDK 提供的一个用于创建锁和其他同步类的基础工具。
 * 它提供了 park() 和 unpark() 方法来阻塞和唤醒线程。
 * </p>
 *
 * <h3>核心特性：</h3>
 * <ul>
 *   <li>park() - 阻塞当前线程</li>
 *   <li>unpark(thread) - 唤醒指定线程</li>
 *   <li>不依赖监视器锁</li>
 *   <li>unpark 可以在 park 之前调用</li>
 *   <li>支持阻塞特定线程</li>
 * </ul>
 *
 * <h3>vs Object.wait()：</h3>
 * <ul>
 *   <li>不需要在 synchronized 块中调用</li>
 *   <li>可以唤醒指定线程</li>
 *   <li>unpark 不会累加许可</li>
 *   <li>响应中断方式不同</li>
 * </ul>
 */
public class LockSupportExample {

    /**
     * 场景1：park/unpark 基础使用
     * <p>
     * 演示 park() 和 unpark() 的基本用法
     * </p>
     */
    public static void parkUnparkBasic() throws InterruptedException {
        System.out.println("=== 场景1：park/unpark 基础 ===");

        Thread worker = new Thread(() -> {
            System.out.println("Worker: 开始执行");
            System.out.println("Worker: 调用 park() 阻塞自己");
            LockSupport.park();
            System.out.println("Worker: 被唤醒，继续执行");
            System.out.println("Worker: 执行完毕");
        });

        worker.start();

        Thread.sleep(2000);
        System.out.println("Main: 唤醒 worker 线程");
        LockSupport.unpark(worker);

        worker.join();
    }

    /**
     * 场景2：unpark 先于 park 调用
     * <p>
     * unpark 可以在 park 之前调用，线程不会阻塞
     * </p>
     */
    public static void unparkBeforePark() throws InterruptedException {
        System.out.println("\n=== 场景2：unpark 先于 park ===");

        Thread worker = new Thread(() -> {
            System.out.println("Worker: 启动，先睡1秒");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Worker: 调用 park()");
            LockSupport.park();
            System.out.println("Worker: 因为之前已经 unpark，所以立即返回");
        });

        worker.start();

        Thread.sleep(500); // 等待线程启动但还没调用 park
        System.out.println("Main: 提前调用 unpark");
        LockSupport.unpark(worker);

        worker.join();
    }

    /**
     * 场景3：park 带超时
     * <p>
     * 演示 parkNanos 和 parkUntil 的使用
     * </p>
     */
    public static void parkWithTimeout() throws InterruptedException {
        System.out.println("\n=== 场景3：park 带超时 ===");

        // parkNanos - 等待指定纳秒数
        Thread thread1 = new Thread(() -> {
            System.out.println("线程1: parkNanos(2秒)");
            long start = System.nanoTime();
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(2));
            long elapsed = System.nanoTime() - start;
            System.out.println("线程1: 唤醒，耗时: " + TimeUnit.NANOSECONDS.toMillis(elapsed) + "ms");
        });

        // parkUntil - 等待到指定截止时间
        Thread thread2 = new Thread(() -> {
            System.out.println("线程2: parkUntil(当前时间+3秒)");
            long deadline = System.currentTimeMillis() + 3000;
            LockSupport.parkUntil(deadline);
            System.out.println("线程2: 唤醒");
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();
    }

    /**
     * 场景4：park 响应中断
     * <p>
     * 演示 park() 如何响应线程中断
     * </p>
     */
    public static void parkWithInterrupt() throws InterruptedException {
        System.out.println("\n=== 场景4：park 响应中断 ===");

        Thread worker = new Thread(() -> {
            System.out.println("Worker: 调用 park() 并等待被中断");
            LockSupport.park();

            // 检查中断状态
            System.out.println("Worker: 被唤醒，检查中断状态");
            System.out.println("Worker: isInterrupted = " + Thread.currentThread().isInterrupted());
            System.out.println("Worker: interrupted = " + Thread.interrupted());
            System.out.println("Worker: isInterrupted (第二次) = " + Thread.currentThread().isInterrupted());
        });

        worker.start();

        Thread.sleep(2000);
        System.out.println("Main: 中断 worker 线程");
        worker.interrupt();

        worker.join();
    }

    /**
     * 场景5：LockSupport vs Object.wait()
     * <p>
     * 对比两种阻塞方式的差异
     * </p>
     */
    public static void lockSupportVsWait() throws InterruptedException {
        System.out.println("\n=== 场景5：LockSupport vs Object.wait() ===");

        // 使用 LockSupport
        System.out.println("--- LockSupport 方式 ---");
        Thread t1 = new Thread(() -> {
            System.out.println("线程1: park()");
            LockSupport.park();
            System.out.println("线程1: 被唤醒");
        });

        // 使用 Object.wait()
        final Object lock = new Object();
        Thread t2 = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("线程2: wait()");
                    lock.wait();
                    System.out.println("线程2: 被唤醒");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        t1.start();
        t2.start();

        Thread.sleep(500);
        System.out.println("主线程: 唤醒 t1 和 t2");
        LockSupport.unpark(t1);
        synchronized (lock) {
            lock.notify();
        }

        t1.join();
        t2.join();
    }

    /**
     * 场景6：多次 unpark 不会累加
     * <p>
     * 多次调用 unpark 不会累加许可，只有一次有效的
     * </p>
     */
    public static void unparkNoAccumulation() throws InterruptedException {
        System.out.println("\n=== 场景6：unpark 不累加 ===");

        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(500);
                System.out.println("Worker: 第一次 park()");
                LockSupport.park();
                System.out.println("Worker: 第一次 park 返回");

                Thread.sleep(500);
                System.out.println("Worker: 第二次 park()");
                LockSupport.park(); // 这次会阻塞
                System.out.println("Worker: 第二次 park 返回");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        worker.start();

        // 提前调用多次 unpark
        System.out.println("Main: unpark 3 次");
        LockSupport.unpark(worker);
        LockSupport.unpark(worker);
        LockSupport.unpark(worker);

        Thread.sleep(2000);
        System.out.println("Main: 再次 unpark 唤醒第二次 park");
        LockSupport.unpark(worker);

        worker.join();
    }

    /**
     * 场景7：使用 Blocker
     * <p>
     * park(Blocker) 可以指定一个 Blocker 对象用于监控
     * </p>
     */
    public static void parkWithBlocker() throws InterruptedException {
        System.out.println("\n=== 场景7：使用 Blocker ===");

        class Blocker {
            private final String name;

            public Blocker(String name) {
                this.name = name;
            }

            @Override
            public String toString() {
                return name;
            }
        }

        Blocker blocker = new Blocker("我的Blocker");

        Thread worker = new Thread(() -> {
            System.out.println("Worker: park(blocker)");
            LockSupport.park(blocker);
            System.out.println("Worker: 被唤醒");

            // 可以通过 Thread 获取 Blocker（需要使用 Unsafe，这里仅演示）
            // 在实际监控工具中可以使用 Thread.getBlocker()
        });

        worker.start();

        Thread.sleep(1000);
        System.out.println("Main: unpark");
        LockSupport.unpark(worker);

        worker.join();
    }

    /**
     * 场景8：实际应用 - 简单的 Future 实现
     * <p>
     * 使用 LockSupport 实现一个简单的 Future
     * </p>
     */
    public static void simpleFutureImplementation() throws InterruptedException {
        System.out.println("\n=== 场景8：简单 Future 实现 ===");

        class SimpleFuture<T> {
            private volatile T result;
            private volatile Thread waiter;

            public void set(T value) {
                this.result = value;
                Thread w = waiter;
                if (w != null) {
                    LockSupport.unpark(w);
                }
            }

            public T get() throws InterruptedException {
                while (result == null) {
                    waiter = Thread.currentThread();
                    LockSupport.park();
                    // 检查是否因中断而唤醒
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }
                return result;
            }
        }

        SimpleFuture<String> future = new SimpleFuture<>();

        // 工作线程
        Thread worker = new Thread(() -> {
            try {
                System.out.println("Worker: 开始计算");
                Thread.sleep(2000);
                String result = "计算结果: " + System.currentTimeMillis();
                System.out.println("Worker: 计算完成，设置结果");
                future.set(result);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 等待结果的线程
        Thread waiter = new Thread(() -> {
            try {
                System.out.println("Waiter: 等待结果...");
                String result = future.get();
                System.out.println("Waiter: 获取到结果 - " + result);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        worker.start();
        waiter.start();

        worker.join();
        waiter.join();
    }

    /**
     * 场景9：FIFO 队列实现
     * <p>
     * 使用 LockSupport 实现一个简单的 FIFO 队列
     * </p>
     */
    public static void fifoQueueImplementation() throws InterruptedException {
        System.out.println("\n=== 场景9：FIFO 队列 ===");

        class FIFOWaitQueue {
            private final ConcurrentLinkedQueue<Thread> waiters = new ConcurrentLinkedQueue<>();

            public void await() throws InterruptedException {
                Thread current = Thread.currentThread();
                waiters.add(current);
                LockSupport.park(this);
                // 移除自己（可能已经取走了）
                waiters.remove(current);

                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }
            }

            public void signal() {
                Thread waiter = waiters.peek();
                if (waiter != null) {
                    LockSupport.unpark(waiter);
                }
            }

            public void signalAll() {
                Thread waiter;
                while ((waiter = waiters.poll()) != null) {
                    LockSupport.unpark(waiter);
                }
            }
        }

        FIFOWaitQueue queue = new FIFOWaitQueue();

        // 创建多个等待线程
        for (int i = 0; i < 3; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    System.out.println("线程 " + id + " 等待");
                    queue.await();
                    System.out.println("线程 " + id + " 被唤醒");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        Thread.sleep(500);
        System.out.println("\n按 FIFO 顺序唤醒:");
        for (int i = 0; i < 3; i++) {
            Thread.sleep(500);
            queue.signal();
        }
    }

    /**
     * 最佳实践与注意事项
     */
    public static void bestPractices() {
        System.out.println("\n=== 最佳实践与注意事项 ===");

        System.out.println("""
        1. LockSupport vs Object.wait()
           - LockSupport 更灵活，不需要在 synchronized 块中
           - LockSupport 可以唤醒指定线程
           - wait/notify 是 Java 语言级别的支持

        2. unpark 可以在 park 之前调用
           - 提前 unpark 不会导致 park 永久阻塞
           - unpark 的"许可"不会累加

        3. park 响应中断
           - park() 会在被中断时返回
           - 需要检查 Thread.interrupted()
           - park() 抛出异常

        4. Blocker 的使用
           - park(Blocker) 可用于监控和诊断
           - 性能分析工具可以获取 Blocker 信息
           - 生产代码一般不需要指定 Blocker

        5. 注意线程状态
           - park() 线程状态为 WAITING
           - parkNanos() 线程状态为 TIMED_WAITING
           - 可以通过 getState() 查看

        6. 适用场景
           - 实现自定义的锁和同步器
           - 需要精确控制线程阻塞/唤醒
           - AQS 内部使用 LockSupport

        7. 不建议在应用层直接使用
           - 一般使用高级同步工具
           - LockSupport 更适合实现底层工具

        8. 与 Unsafe 的关系
           - LockSupport 是 Unsafe.park/unpark 的封装
           - 提供了更安全的 API
        """);
    }

    public static void main(String[] args) throws InterruptedException {
        parkUnparkBasic();
        unparkBeforePark();
        parkWithTimeout();
        parkWithInterrupt();
        lockSupportVsWait();
        unparkNoAccumulation();
        parkWithBlocker();
        simpleFutureImplementation();
        fifoQueueImplementation();
        bestPractices();
    }
}
