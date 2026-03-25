package com.example.juc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ThreadLocal学习示例
 * 线程本地变量 - 每个线程都有自己独立的变量副本
 */
public class ThreadLocalExample {

    // 1. 基本ThreadLocal
    private static ThreadLocal<Integer> threadLocalValue = ThreadLocal.withInitial(() -> 0);

    // 2. 用户上下文示例
    private static ThreadLocal<UserContext> userContext = new ThreadLocal<>();

    // 3. SimpleDateFormat线程安全示例
    private static ThreadLocal<java.text.SimpleDateFormat> dateFormat =
        ThreadLocal.withInitial(() -> new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ThreadLocal学习示例 ===\n");

        // 1. 基本使用
        System.out.println("1. 基本使用:");
        basicUsage();

        // 2. 线程隔离演示
        System.out.println("\n2. 线程隔离演示:");
        threadIsolation();

        // 3. 实际场景 - 用户上下文
        System.out.println("\n3. 实际场景 - 用户上下文:");
        userContextScenario();

        // 4. InheritableThreadLocal
        System.out.println("\n4. InheritableThreadLocal - 子线程继承父线程值:");
        inheritableThreadLocalDemo();

        // 5. 内存泄漏问题演示
        System.out.println("\n5. 内存泄漏与正确清理:");
        memoryLeakDemo();
    }

    // 1. 基本使用
    private static void basicUsage() throws InterruptedException {
        ThreadLocal<String> local = ThreadLocal.withInitial(() -> "默认值");

        System.out.println("主线程初始值: " + local.get());

        local.set("主线程的值");
        System.out.println("主线程设置后: " + local.get());

        Thread otherThread = new Thread(() -> {
            System.out.println("  子线程初始值: " + local.get()); // 独立副本
            local.set("子线程的值");
            System.out.println("  子线程设置后: " + local.get());
        });

        otherThread.start();
        otherThread.join();

        System.out.println("主线程的值未受影响: " + local.get());

        local.remove(); // 清理
    }

    // 2. 线程隔离演示
    private static void threadIsolation() throws InterruptedException {
        ThreadLocal<Integer> counter = ThreadLocal.withInitial(() -> 0);

        Thread[] threads = new Thread[3];

        for (int i = 0; i < threads.length; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                counter.set(0);
                System.out.println("  线程" + threadId + "初始值: " + counter.get());

                for (int j = 0; j < 5; j++) {
                    counter.set(counter.get() + 1);
                }

                System.out.println("  线程" + threadId + "最终值: " + counter.get());
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
    }

    // 3. 实际场景 - 用户上下文
    private static void userContextScenario() {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // 模拟多个请求
        for (int i = 1; i <= 3; i++) {
            final int userId = i;
            executor.submit(() -> {
                // 设置当前线程的用户上下文
                userContext.set(new UserContext(userId, "用户" + userId));

                // 多个方法调用，都能访问到用户上下文
                serviceA();
                serviceB();
                serviceC();

                // 重要：清理ThreadLocal，避免内存泄漏
                userContext.remove();
            });
        }

        executor.shutdown();
    }

    private static void serviceA() {
        UserContext ctx = userContext.get();
        System.out.println("  ServiceA处理用户: " + ctx.getUserId());
    }

    private static void serviceB() {
        UserContext ctx = userContext.get();
        System.out.println("  ServiceB处理用户: " + ctx.getUserName());
    }

    private static void serviceC() {
        UserContext ctx = userContext.get();
        System.out.println("  ServiceC完成处理");
    }

    // 4. InheritableThreadLocal
    private static void inheritableThreadLocalDemo() throws InterruptedException {
        InheritableThreadLocal<String> inheritableLocal = new InheritableThreadLocal<>();
        inheritableLocal.set("父线程的值");

        System.out.println("  父线程: " + inheritableLocal.get());

        Thread childThread = new Thread(() -> {
            System.out.println("  子线程继承的值: " + inheritableLocal.get());
            inheritableLocal.set("子线程修改后的值");
            System.out.println("  子线程修改后: " + inheritableLocal.get());
        });

        childThread.start();
        childThread.join();

        System.out.println("  父线程的值: " + inheritableLocal.get());
    }

    // 5. 内存泄漏演示
    private static void memoryLeakDemo() throws InterruptedException {
        // 使用线程池演示
        ExecutorService pool = Executors.newFixedThreadPool(2);

        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            pool.submit(() -> {
                threadLocalValue.set(taskId * 100);
                System.out.println("  任务" + taskId + "执行中, ThreadLocal值: " + threadLocalValue.get());

                // 模拟工作
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // ✅ 正确做法：使用后清理
                threadLocalValue.remove();
                System.out.println("  任务" + taskId + "清理ThreadLocal");
            });
        }

        Thread.sleep(1000);
        pool.shutdown();
    }

    // 用户上下文类
    static class UserContext {
        private final int userId;
        private final String userName;

        public UserContext(int userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }

        public int getUserId() {
            return userId;
        }

        public String getUserName() {
            return userName;
        }
    }
}
