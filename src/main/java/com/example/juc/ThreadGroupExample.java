package com.example.juc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ThreadGroup 使用示例
 * <p>
 * 演示 ThreadGroup 的使用方法和注意事项
 * </p>
 *
 * <h3>ThreadGroup 特性：</h3>
 * <ul>
 *   <li>线程的树形组织结构</li>
   <li>可以批量管理线程</li>
   <li>可以统一设置异常处理器</li>
   * <li>可以统一中断组内线程</li>
 * </ul>
 *
 * <h3>注意：</h3>
 * <ul>
 *   <li>现代应用中较少使用 ThreadGroup</li>
 *   <li>ExecutorService 提供了更好的线程管理</li>
 *   <li>ThreadGroup 主要用于特定场景</li>
 * </ul>
 */
public class ThreadGroupExample {

    /**
     * 场景1：ThreadGroup 基础使用
     * <p>
     * 演示如何创建 ThreadGroup 并添加线程
     * </p>
     */
    public static void basicThreadGroup() throws InterruptedException {
        System.out.println("=== 场景1：ThreadGroup 基础使用 ===");

        ThreadGroup group = new ThreadGroup("我的工作组");
        System.out.println("线程组名称: " + group.getName());
        System.out.println("线程组优先级: " + group.getMaxPriority());
        System.out.println("是否为守护线程组: " + group.isDaemon());

        // 创建属于该组的线程
        Thread t1 = new Thread(group, () -> {
            System.out.println(Thread.currentThread().getName() +
                    " 在线程组 " + Thread.currentThread().getThreadGroup().getName() + " 中运行");
        }, "Worker-1");

        Thread t2 = new Thread(group, () -> {
            System.out.println(Thread.currentThread().getName() +
                    " 在线程组 " + Thread.currentThread().getThreadGroup().getName() + " 中运行");
        }, "Worker-2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("活动线程数: " + group.activeCount());
    }

    /**
     * 场景2：嵌套 ThreadGroup
     * <p>
     * 演示线程组的树形结构
     * </p>
     */
    public static void nestedThreadGroup() throws InterruptedException {
        System.out.println("\n=== 场景2：嵌套 ThreadGroup ===");

        ThreadGroup rootGroup = new ThreadGroup("根组");
        ThreadGroup childGroup1 = new ThreadGroup(rootGroup, "子组1");
        ThreadGroup childGroup2 = new ThreadGroup(rootGroup, "子组2");
        ThreadGroup grandChildGroup = new ThreadGroup(childGroup1, "孙组");

        // 在各个组中创建线程
        Thread t1 = new Thread(rootGroup, () -> sleepAndPrint("根组线程"), "RootThread");
        Thread t2 = new Thread(childGroup1, () -> sleepAndPrint("子组1线程"), "Child1Thread");
        Thread t3 = new Thread(childGroup2, () -> sleepAndPrint("子组2线程"), "Child2Thread");
        Thread t4 = new Thread(grandChildGroup, () -> sleepAndPrint("孙组线程"), "GrandChildThread");

        t1.start();
        t2.start();
        t3.start();
        t4.start();

        Thread.sleep(500);

        System.out.println("\n线程组层次结构:");
        printThreadGroupInfo(rootGroup, 0);

        t1.join();
        t2.join();
        t3.join();
        t4.join();
    }

    /**
     * 场景3：批量中断线程组
     * <p>
     * 演示如何中断组内的所有线程
     * </p>
     */
    public static void interruptThreadGroup() throws InterruptedException {
        System.out.println("\n=== 场景3：批量中断线程组 ===");

        ThreadGroup group = new ThreadGroup("工作组");

        // 创建长时间运行的线程
        for (int i = 0; i < 3; i++) {
            final int id = i;
            new Thread(group, () -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        System.out.println("线程 " + id + " 正在运行...");
                        Thread.sleep(500);
                    }
                    System.out.println("线程 " + id + " 检测到中断，退出");
                } catch (InterruptedException e) {
                    System.out.println("线程 " + id + " 被中断");
                    Thread.currentThread().interrupt();
                }
            }, "Worker-" + id).start();
        }

        Thread.sleep(1500);
        System.out.println("\n中断线程组中的所有线程...");
        group.interrupt();

        Thread.sleep(1000);
        System.out.println("活动线程数: " + group.activeCount());
    }

    /**
     * 场景4：统一异常处理
     * <p>
     * 演示通过 ThreadGroup 统一处理异常
     * </p>
     */
    public static void unifiedExceptionHandling() throws InterruptedException {
        System.out.println("\n=== 场景4：统一异常处理 ===");

        class ExceptionHandlingThreadGroup extends ThreadGroup {
            private final AtomicInteger exceptionCount = new AtomicInteger(0);

            public ExceptionHandlingThreadGroup(String name) {
                super(name);
            }

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                int count = exceptionCount.incrementAndGet();
                System.out.println("【线程组异常处理器】#" + count);
                System.out.println("  线程: " + t.getName());
                System.out.println("  组: " + getName());
                System.out.println("  异常: " + e.getMessage());
            }

            public int getExceptionCount() {
                return exceptionCount.get();
            }
        }

        ExceptionHandlingThreadGroup group = new ExceptionHandlingThreadGroup("异常处理组");

        // 创建会抛出异常的线程
        for (int i = 0; i < 3; i++) {
            final int id = i;
            new Thread(group, () -> {
                throw new RuntimeException("线程 " + id + " 的异常");
            }, "ExceptionThread-" + id).start();
        }

        Thread.sleep(500);
        System.out.println("共处理异常数: " + group.getExceptionCount());
    }

    /**
     * 场景5：设置线程组优先级
     * <p>
     * 演示线程组优先级对组内线程的影响
     * </p>
     */
    public static void threadGroupPriority() throws InterruptedException {
        System.out.println("\n=== 场景5：线程组优先级 ===");

        ThreadGroup highPriorityGroup = new ThreadGroup("高优先级组");
        highPriorityGroup.setMaxPriority(Thread.MAX_PRIORITY);

        ThreadGroup lowPriorityGroup = new ThreadGroup("低优先级组");
        lowPriorityGroup.setMaxPriority(Thread.MIN_PRIORITY + 2);

        Thread highThread = new Thread(highPriorityGroup, () -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("高优先级线程: " + i);
            }
        });

        Thread lowThread = new Thread(lowPriorityGroup, () -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("低优先级线程: " + i);
            }
        });

        System.out.println("高优先级线程优先级: " + highThread.getPriority());
        System.out.println("低优先级线程优先级: " + lowThread.getPriority());

        highThread.start();
        lowThread.start();

        highThread.join();
        lowThread.join();
    }

    /**
     * 场景6：线程组信息查询
     * <p>
     * 演示如何查询线程组的各种信息
     * </p>
     */
    public static void threadGroupInfo() throws InterruptedException {
        System.out.println("\n=== 场景6：线程组信息查询 ===");

        ThreadGroup mainGroup = Thread.currentThread().getThreadGroup();

        System.out.println("主线程组名称: " + mainGroup.getName());
        System.out.println("主线程组父组: " + mainGroup.getParent().getName());
        System.out.println("活动线程数: " + mainGroup.activeCount());
        System.out.println("活动子组数: " + mainGroup.activeGroupCount());

        System.out.println("\n枚举所有活动线程:");
        Thread[] threads = new Thread[mainGroup.activeCount()];
        mainGroup.enumerate(threads);
        for (Thread t : threads) {
            if (t != null) {
                System.out.println("  " + t.getName() +
                        " (状态: " + t.getState() + ")");
            }
        }

        System.out.println("\n枚举所有子组:");
        ThreadGroup[] groups = new ThreadGroup[mainGroup.activeGroupCount()];
        mainGroup.enumerate(groups);
        for (ThreadGroup g : groups) {
            if (g != null) {
                System.out.println("  " + g.getName());
            }
        }

        // 递归打印所有线程
        System.out.println("\n递归打印线程组树:");
        printThreadGroupTree(mainGroup, 0);
    }

    /**
     * 场景7：守护线程组
     * <p>
     * 演示守护线程组的特性
     * </p>
     */
    public static void daemonThreadGroup() throws InterruptedException {
        System.out.println("\n=== 场景7：守护线程组 ===");

        ThreadGroup daemonGroup = new ThreadGroup("守护线程组");
        daemonGroup.setDaemon(true);

        Thread normalThread = new Thread(daemonGroup, () -> {
            try {
                for (int i = 0; i < 3; i++) {
                    System.out.println("守护组中的线程: " + i);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        System.out.println("是否为守护线程组: " + daemonGroup.isDaemon());

        normalThread.start();

        // 由于守护线程组没有活动线程时会被销毁
        // 等待线程完成
        normalThread.join();

        System.out.println("线程完成后，守护线程组应该不存在了");
    }

    /**
     * 场景8：使用 ThreadGroup 实现任务组管理
     * <p>
     * 演示一个实际场景：批量任务管理和监控
     * </p>
     */
    public static void taskGroupManagement() throws InterruptedException {
        System.out.println("\n=== 场景8：任务组管理 ===");

        class TaskGroup {
            private final ThreadGroup group;
            private final CountDownLatch completionLatch;

            public TaskGroup(String name, int taskCount) {
                this.group = new ThreadGroup(name);
                this.completionLatch = new CountDownLatch(taskCount);
            }

            public void submitTask(Runnable task, String taskName) {
                Thread thread = new Thread(group, () -> {
                    try {
                        task.run();
                    } finally {
                        completionLatch.countDown();
                    }
                }, taskName);
                thread.start();
            }

            public void awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
                if (!completionLatch.await(timeout, unit)) {
                    System.out.println("超时，取消所有任务");
                    cancelAll();
                }
            }

            public void cancelAll() {
                group.interrupt();
            }

            public int getActiveThreadCount() {
                return group.activeCount();
            }

            public ThreadGroup getGroup() {
                return group;
            }
        }

        TaskGroup taskGroup = new TaskGroup("批量任务组", 5);

        // 提交任务
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            taskGroup.submitTask(() -> {
                try {
                    System.out.println("任务 " + taskId + " 开始执行");
                    Thread.sleep(ThreadLocalRandom.current().nextInt(500, 2000));
                    System.out.println("任务 " + taskId + " 完成");
                } catch (InterruptedException e) {
                    System.out.println("任务 " + taskId + " 被取消");
                    Thread.currentThread().interrupt();
                }
            }, "Task-" + taskId);
        }

        // 监控任务执行
        Thread monitor = new Thread(() -> {
            int count = 0;
            while (taskGroup.getActiveThreadCount() > 0 && count < 10) {
                System.out.println("监控: 活跃线程数 = " + taskGroup.getActiveThreadCount());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                count++;
            }
        });
        monitor.setDaemon(true);
        monitor.start();

        taskGroup.awaitCompletion(10, TimeUnit.SECONDS);

        System.out.println("所有任务处理完成或超时");
    }

    /**
     * 辅助方法
     */
    private static void sleepAndPrint(String message) {
        try {
            Thread.sleep(200);
            System.out.println(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void printThreadGroupInfo(ThreadGroup group, int indent) {
        String prefix = "  ".repeat(indent);
        System.out.println(prefix + "├─ " + group.getName() +
                " (线程: " + group.activeCount() + ", 子组: " + group.activeGroupCount() + ")");

        ThreadGroup[] groups = new ThreadGroup[group.activeGroupCount()];
        group.enumerate(groups);
        for (ThreadGroup g : groups) {
            if (g != null) {
                printThreadGroupInfo(g, indent + 1);
            }
        }
    }

    private static void printThreadGroupTree(ThreadGroup group, int indent) {
        String prefix = "  ".repeat(indent);
        System.out.println(prefix + "├─ " + group.getName());

        // 打印线程
        Thread[] threads = new Thread[group.activeCount()];
        group.enumerate(threads, false);
        for (Thread t : threads) {
            if (t != null) {
                System.out.println(prefix + "│   └─ " + t.getName());
            }
        }

        // 递归打印子组
        ThreadGroup[] groups = new ThreadGroup[group.activeGroupCount()];
        group.enumerate(groups);
        for (ThreadGroup g : groups) {
            if (g != null) {
                printThreadGroupTree(g, indent + 1);
            }
        }
    }

    /**
     * 最佳实践与注意事项
     */
    public static void bestPractices() {
        System.out.println("\n=== 最佳实践与注意事项 ===");

        System.out.println("""
        1. ThreadGroup 的适用场景
           - 需要批量管理线程
           - 需要统一异常处理
           - 需要统一的线程配置

        2. 现代应用中的替代方案
           - 优先使用 ExecutorService
           - 使用 CompletableFuture
           - 使用虚拟线程（Java 21+）

        3. 注意事项
           - ThreadGroup 功能相对有限
           - API 设计较老
           - 不支持更多高级特性

        4. 统一异常处理
           - 可以覆盖 uncaughtException
           - 适合收集异常日志
           - 可以发送告警

        5. 批量中断
           - 使用 interrupt() 中断所有线程
           - 线程需要正确处理中断
           - 确保资源被清理

        6. 线程组层次结构
           - 形成树形结构
           - 可以递归查询
           - 根组是 main

        7. 守护线程组
           - 没有活动线程时会被销毁
           - 适合临时任务
           - 不会阻止 JVM 退出

        8. 线程组优先级
           - 限制组内线程的最大优先级
           - 不能设置超过组的优先级
           - 谨慎使用优先级

        9. 性能考虑
           - 线程组操作本身有开销
           - 大量线程组可能影响性能
           - 合理设计层次结构

        10. 监控和管理
            - 定期检查活跃线程数
            - 监控子组状态
            - 及时清理无用组
        """);
    }

    public static void main(String[] args) throws InterruptedException {
        basicThreadGroup();
        nestedThreadGroup();
        interruptThreadGroup();
        unifiedExceptionHandling();
        threadGroupPriority();
        threadGroupInfo();
        daemonThreadGroup();
        taskGroupManagement();
        bestPractices();
    }
}
