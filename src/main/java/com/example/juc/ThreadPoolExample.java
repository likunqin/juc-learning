package com.example.juc;

import java.util.concurrent.*;

/**
 * 线程池学习示例
 * 演示各种线程池的使用
 */
public class ThreadPoolExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 线程池学习示例 ===");

        // 1. FixedThreadPool - 固定大小线程池
        System.out.println("\n1. FixedThreadPool示例:");
        ExecutorService fixedPool = Executors.newFixedThreadPool(3);

        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            fixedPool.submit(() -> {
                System.out.println("任务 " + taskId + " 由 " +
                        Thread.currentThread().getName() + " 执行");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        fixedPool.shutdown();
        fixedPool.awaitTermination(5, TimeUnit.SECONDS);

        // 2. CachedThreadPool - 缓存线程池
        System.out.println("\n2. CachedThreadPool示例:");
        ExecutorService cachedPool = Executors.newCachedThreadPool();

        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            cachedPool.submit(() -> {
                System.out.println("缓存池任务 " + taskId + " 由 " +
                        Thread.currentThread().getName() + " 执行");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        cachedPool.shutdown();
        cachedPool.awaitTermination(2, TimeUnit.SECONDS);

        // 3. ScheduledThreadPool - 定时任务线程池
        System.out.println("\n3. ScheduledThreadPool示例:");
        ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(2);

        // 延迟执行
        scheduledPool.schedule(() -> {
            System.out.println("延迟任务执行 - " + System.currentTimeMillis());
        }, 2, TimeUnit.SECONDS);

        // 定期执行
        scheduledPool.scheduleAtFixedRate(() -> {
            System.out.println("定期任务执行 - " + System.currentTimeMillis());
        }, 1, 2, TimeUnit.SECONDS);

        // 让程序运行一会儿
        Thread.sleep(6000);
        scheduledPool.shutdown();

        // 4. ForkJoinPool - 分治任务线程池
        System.out.println("\n4. ForkJoinPool示例:");
        ForkJoinPool forkJoinPool = new ForkJoinPool();

        // 计算1到1000的和
        long result = forkJoinPool.invoke(new RecursiveTaskExample(1, 1000));
        System.out.println("1到1000的和: " + result);

        forkJoinPool.shutdown();
    }

    // 递归任务示例
    static class RecursiveTaskExample extends RecursiveTask<Long> {
        private final int start;
        private final int end;
        private static final int THRESHOLD = 100;

        public RecursiveTaskExample(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            if (end - start <= THRESHOLD) {
                // 直接计算
                long sum = 0;
                for (int i = start; i <= end; i++) {
                    sum += i;
                }
                return sum;
            } else {
                // 分割任务
                int middle = (start + end) / 2;
                RecursiveTaskExample left = new RecursiveTaskExample(start, middle);
                RecursiveTaskExample right = new RecursiveTaskExample(middle + 1, end);

                left.fork();
                long rightResult = right.compute();
                long leftResult = left.join();

                return leftResult + rightResult;
            }
        }
    }
}