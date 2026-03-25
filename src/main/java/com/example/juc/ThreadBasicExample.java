package com.example.juc;

/**
 * 线程基础学习示例
 * Thread、Runnable、Callable、线程状态、生命周期
 */
public class ThreadBasicExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 线程基础学习示例 ===\n");

        // 1. 创建线程的三种方式
        System.out.println("1. 创建线程的三种方式:");
        createThread();

        // 2. 线程优先级
        System.out.println("\n2. 线程优先级:");
        threadPriority();

        // 3. 守护线程
        System.out.println("\n3. 守护线程:");
        daemonThread();

        // 4. 线程状态
        System.out.println("\n4. 线程状态:");
        threadStates();

        // 5. 线程中断
        System.out.println("\n5. 线程中断:");
        threadInterrupt();

        // 6. join等待
        System.out.println("\n6. join等待线程结束:");
        joinDemo();

        // 7. yield让步
        System.out.println("\n7. yield让步:");
        yieldDemo();
    }

    // 1. 创建线程的三种方式
    private static void createThread() {
        // 方式1: 继承Thread类
        Thread t1 = new Thread("Thread-方式1") {
            @Override
            public void run() {
                System.out.println("  方式1: 继承Thread类 - " + getName());
            }
        };

        // 方式2: 实现Runnable接口
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("  方式2: 实现Runnable接口 - " + Thread.currentThread().getName());
            }
        }, "Thread-方式2");

        // 方式3: Lambda表达式 (推荐)
        Thread t3 = new Thread(() -> {
            System.out.println("  方式3: Lambda表达式 - " + Thread.currentThread().getName());
        }, "Thread-方式3");

        t1.start();
        t2.start();
        t3.start();

        // 等待线程完成
        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 方式4: 使用FutureTask + Callable
        System.out.println("  方式4: FutureTask + Callable:");
        java.util.concurrent.FutureTask<Integer> futureTask =
            new java.util.concurrent.FutureTask<>(() -> {
                System.out.println("    Callable任务执行");
                return 42;
            });
        new Thread(futureTask, "Callable-Thread").start();

        try {
            Integer result = futureTask.get();
            System.out.println("    Callable返回结果: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 2. 线程优先级
    private static void threadPriority() {
        System.out.println("  线程优先级范围: " + Thread.MIN_PRIORITY + " ~ " + Thread.MAX_PRIORITY);
        System.out.println("  默认优先级: " + Thread.NORM_PRIORITY);

        Thread high = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("  高优先级线程: " + i);
            }
        }, "High-Priority");
        high.setPriority(Thread.MAX_PRIORITY);

        Thread low = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("  低优先级线程: " + i);
            }
        }, "Low-Priority");
        low.setPriority(Thread.MIN_PRIORITY);

        high.start();
        low.start();

        try {
            high.join();
            low.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("  注意: 优先级只是建议，不保证执行顺序");
    }

    // 3. 守护线程
    private static void daemonThread() throws InterruptedException {
        Thread daemonThread = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    System.out.println("  守护线程执行: " + i);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Daemon-Thread");

        daemonThread.setDaemon(true); // 设置为守护线程
        daemonThread.start();

        System.out.println("  主线程休眠2秒后结束");
        Thread.sleep(2000);
        System.out.println("  主线程结束，守护线程也会随之结束");
    }

    // 4. 线程状态
    private static void threadStates() throws InterruptedException {
        Object lockObj = new Object();
        Thread stateThread = new Thread(() -> {
            try {
                // 状态: RUNNABLE
                System.out.println("  线程状态: RUNNABLE");

                // 状态: TIMED_WAITING (sleep)
                Thread.sleep(1000);
                System.out.println("  sleep后线程状态: RUNNABLE");

                // 状态: WAITING (wait)
                synchronized (lockObj) {
                    lockObj.wait(500);
                }
                System.out.println("  wait后线程状态: RUNNABLE");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        System.out.println("  线程状态: " + stateThread.getState()); // NEW
        stateThread.start();
        System.out.println("  线程状态: " + stateThread.getState()); // RUNNABLE

        Thread.sleep(100);
        System.out.println("  线程状态: " + stateThread.getState()); // 可能是RUNNABLE或TIMED_WAITING

        stateThread.join();
        System.out.println("  线程状态: " + stateThread.getState()); // TERMINATED

        System.out.println("\n  线程状态总结:");
        System.out.println("    NEW - 新创建，未启动");
        System.out.println("    RUNNABLE - 正在运行或等待CPU");
        System.out.println("    BLOCKED - 等待监视器锁");
        System.out.println("    WAITING - 无限期等待 (wait, join, park)");
        System.out.println("    TIMED_WAITING - 有时限等待 (sleep, wait(timeout))");
        System.out.println("    TERMINATED - 线程已结束");
    }

    // 5. 线程中断
    private static void threadInterrupt() throws InterruptedException {
        Thread worker = new Thread(() -> {
            System.out.println("  工作线程启动，检查中断状态...");

            // 方式1: 检查中断标志
            while (!Thread.currentThread().isInterrupted()) {
                // 模拟工作
                try {
                    System.out.println("  工作中...");
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println("  捕获到InterruptedException");
                    System.out.println("  中断标志被清除: " + Thread.currentThread().isInterrupted());
                    // 恢复中断状态
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("  工作线程退出");
        });

        worker.start();
        Thread.sleep(1500);
        System.out.println("  主线程请求中断工作线程");
        worker.interrupt();
        worker.join();
    }

    // 6. join等待
    private static void joinDemo() throws InterruptedException {
        Thread t1 = new Thread(() -> {
            try {
                System.out.println("  任务1开始");
                Thread.sleep(1000);
                System.out.println("  任务1完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                System.out.println("  任务2开始");
                Thread.sleep(800);
                System.out.println("  任务2完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        t1.start();
        t2.start();

        // join等待线程结束
        t1.join();
        t2.join();

        System.out.println("  两个任务都已完成，主线程继续");

        // join带超时
        Thread t3 = new Thread(() -> {
            try {
                Thread.sleep(2000);
                System.out.println("  任务3完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        t3.start();
        t3.join(1000); // 最多等1秒
        System.out.println("  等待1秒后主线程继续");
    }

    // 7. yield让步
    private static void yieldDemo() {
        Thread highYield = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                if (i % 2 == 0) {
                    Thread.yield(); // 让出CPU
                }
                System.out.println("  Yield线程A: " + i);
            }
        }, "Yield-A");

        Thread lowYield = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                System.out.println("  Yield线程B: " + i);
            }
        }, "Yield-B");

        highYield.start();
        lowYield.start();

        try {
            highYield.join();
            lowYield.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("  注意: yield只是建议，不保证立即让出CPU");
    }
}
