package com.example.juc;

import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Future和FutureTask学习示例
 * 异步任务的基础API
 */
public class FutureExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Future学习示例 ===\n");

        // 1. Future基本使用
        System.out.println("1. Future基本使用:");
        basicFuture();

        // 2. FutureTask使用
        System.out.println("\n2. FutureTask使用:");
        futureTaskUsage();

        // 3. 超时控制
        System.out.println("\n3. 超时控制:");
        timeoutControl();

        // 4. 取消任务
        System.out.println("\n4. 取消任务:");
        cancelTask();

        // 5. 批量任务处理
        System.out.println("\n5. 批量任务处理 - invokeAll:");
        batchTasks();

        // 6. 获取最先完成的任务
        System.out.println("\n6. 获取最先完成的任务 - invokeAny:");
        firstCompletedTask();
    }

    // 1. Future基本使用
    private static void basicFuture() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 提交有返回值的任务
        Future<Integer> future = executor.submit(() -> {
            System.out.println("  任务开始执行...");
            Thread.sleep(1000);
            return 42;
        });

        // 检查任务状态
        System.out.println("  任务是否完成: " + future.isDone());

        // 获取结果（阻塞）
        Integer result = future.get();
        System.out.println("  任务结果: " + result);

        System.out.println("  任务是否完成: " + future.isDone());

        executor.shutdown();
    }

    // 2. FutureTask使用
    private static void futureTaskUsage() throws Exception {
        // 创建一个FutureTask
        FutureTask<String> futureTask = new FutureTask<>(() -> {
            System.out.println("  FutureTask执行中...");
            Thread.sleep(800);
            return "FutureTask结果";
        });

        // 可以在多个地方提交同一个FutureTask（只会执行一次）
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(futureTask);

        // 主线程也可以执行
        // new Thread(futureTask).start();

        // 获取结果
        String result = futureTask.get();
        System.out.println("  结果: " + result);

        executor.shutdown();
    }

    // 3. 超时控制
    private static void timeoutControl() {
        ExecutorService executor = Executors.newFixedThreadPool(1);

        Future<String> future = executor.submit(() -> {
            Thread.sleep(3000); // 模拟长时间任务
            return "完成";
        });

        try {
            // 等待最多1秒
            String result = future.get(1, TimeUnit.SECONDS);
            System.out.println("  获取到结果: " + result);
        } catch (TimeoutException e) {
            System.out.println("  任务超时: " + e.getMessage());

            // 超时后取消任务
            future.cancel(true); // true表示如果任务正在执行，尝试中断
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("  任务是否被取消: " + future.isCancelled());
        System.out.println("  任务是否完成: " + future.isDone());

        executor.shutdown();
    }

    // 4. 取消任务
    private static void cancelTask() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(1);

        Future<Integer> future = executor.submit(() -> {
            for (int i = 0; i < 10; i++) {
                System.out.println("  任务执行中: " + i);
                Thread.sleep(500);
            }
            return 100;
        });

        // 让任务运行一会儿
        Thread.sleep(1200);

        // 取消任务
        boolean cancelled = future.cancel(true);
        System.out.println("  取消结果: " + cancelled);

        System.out.println("  任务是否被取消: " + future.isCancelled());

        try {
            // 如果任务被取消，get会抛出CancellationException
            future.get();
        } catch (CancellationException e) {
            System.out.println("  捕获取消异常: " + e.getClass().getSimpleName());
        }

        executor.shutdown();
    }

    // 5. 批量任务处理
    private static void batchTasks() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            tasks.add(() -> {
                Thread.sleep(taskId * 200);
                return taskId * 100;
            });
        }

        long startTime = System.currentTimeMillis();

        // 执行所有任务，等待全部完成
        List<Future<Integer>> futures = executor.invokeAll(tasks);

        long endTime = System.currentTimeMillis();

        System.out.println("  总耗时: " + (endTime - startTime) + "ms");

        // 收集结果
        List<Integer> results = new ArrayList<>();
        for (Future<Integer> future : futures) {
            results.add(future.get());
        }

        System.out.println("  所有任务结果: " + results);

        executor.shutdown();
    }

    // 6. 获取最先完成的任务
    private static void firstCompletedTask() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);

        List<Callable<String>> tasks = List.of(
            () -> { Thread.sleep(1000); return "慢任务"; },
            () -> { Thread.sleep(200); return "快任务"; },
            () -> { Thread.sleep(500); return "中等任务"; }
        );

        long startTime = System.currentTimeMillis();

        // 返回最先完成的任务结果，其他任务被取消
        String result = executor.invokeAny(tasks);

        long endTime = System.currentTimeMillis();

        System.out.println("  最先完成的任务: " + result);
        System.out.println("  耗时: " + (endTime - startTime) + "ms");

        executor.shutdown();
    }
}
