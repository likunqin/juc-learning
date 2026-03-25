package com.example.juc;

import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.LongStream;

/**
 * ForkJoinPool深度剖析
 * 工作窃取算法和分治任务
 */
public class ForkJoinPoolDeepDive {

    public static void main(String[] args) {
        System.out.println("=== ForkJoinPool深度剖析 ===\n");

        // 1. 基本原理
        System.out.println("1. ForkJoinPool工作原理:");
        basicPrinciple();

        // 2. 大任务分解
        System.out.println("\n2. 大任务分解 - 数组求和:");
        largeTask();

        // 3. 并行流使用
        System.out.println("\n3. 并行流 vs ForkJoinPool:");
        parallelStream();

        // 4. 自定义任务类型
        System.out.println("\n4. RecursiveAction vs RecursiveTask:");
        taskTypes();

        // 5. 与普通线程池对比
        System.out.println("\n5. ForkJoinPool vs 普通线程池:");
        comparison();

        // 6. 最佳实践
        System.out.println("\n6. ForkJoinPool最佳实践:");
        bestPractices();
    }

    // 1. 基本原理
    private static void basicPrinciple() {
        System.out.println("  ForkJoinPool特点:");
        System.out.println("    ✓ 基于工作窃取算法");
        System.out.println("    ✓ 每个工作线程有自己的任务队列");
        System.out.println("    ✓ 队列为双端队列（Deque）");
        System.out.println("    ✓ 工作线程从队尾获取任务（LIFO）");
        System.out.println("    ✓ 空闲线程从其他队列队首窃取任务（FIFO）");
        System.out.println("    ✓ 适合分治算法");
        System.out.println();
        System.out.println("  核心方法:");
        System.out.println("    fork() - 异步执行子任务");
        System.out.println("    join() - 等待子任务完成并获取结果");
        System.out.println("    invoke() - 同步执行任务");
    }

    // 2. 大任务分解
    private static void largeTask() {
        long[] array = LongStream.rangeClosed(1, 10_000_000).toArray();

        ForkJoinPool pool = new ForkJoinPool();
        long startTime = System.currentTimeMillis();

        Long result = pool.invoke(new SumTask(array, 0, array.length));

        long endTime = System.currentTimeMillis();

        System.out.println("  1到1000万求和: " + result);
        System.out.println("  耗时: " + (endTime - startTime) + "ms");
        System.out.println("  预期结果: " + (10_000_000L * (10_000_000L + 1) / 2));

        pool.shutdown();
    }

    // 求和任务
    static class SumTask extends RecursiveTask<Long> {
        private final long[] array;
        private final int start;
        private final int end;
        private static final int THRESHOLD = 10_000;

        public SumTask(long[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            if (end - start <= THRESHOLD) {
                // 任务足够小，直接计算
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += array[i];
                }
                return sum;
            } else {
                // 任务太大，分解
                int middle = (start + end) / 2;
                SumTask left = new SumTask(array, start, middle);
                SumTask right = new SumTask(array, middle, end);

                // 异步执行左任务
                left.fork();

                // 同步执行右任务
                Long rightResult = right.compute();

                // 等待左任务完成
                Long leftResult = left.join();

                return leftResult + rightResult;
            }
        }
    }

    // 3. 并行流
    private static void parallelStream() {
        List<Integer> list = new ArrayList<>();
        for (int i = 1; i <= 1_000_000; i++) {
            list.add(i);
        }

        // 普通流
        long start1 = System.currentTimeMillis();
        long sum1 = list.stream().mapToLong(Integer::longValue).sum();
        long time1 = System.currentTimeMillis() - start1;

        // 并行流（使用ForkJoinPool）
        long start2 = System.currentTimeMillis();
        long sum2 = list.parallelStream().mapToLong(Integer::longValue).sum();
        long time2 = System.currentTimeMillis() - start2;

        System.out.println("  普通流求和: " + sum1 + ", 耗时: " + time1 + "ms");
        System.out.println("  并行流求和: " + sum2 + ", 耗时: " + time2 + "ms");
        System.out.println("  性能提升: " + ((double)time1 / time2) + "x");
    }

    // 4. 任务类型对比
    private static void taskTypes() {
        ForkJoinPool pool = new ForkJoinPool();

        System.out.println("  RecursiveTask - 有返回值:");
        SumTask sumTask = new SumTask(new long[]{1, 2, 3, 4, 5}, 0, 5);
        Long result = pool.invoke(sumTask);
        System.out.println("    求和结果: " + result);

        System.out.println("  RecursiveAction - 无返回值:");
        PrintTask printTask = new PrintTask(1, 5);
        pool.invoke(printTask);

        pool.shutdown();
    }

    // 打印任务（无返回值）
    static class PrintTask extends RecursiveAction {
        private final int start;
        private final int end;
        private static final int THRESHOLD = 2;

        public PrintTask(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                for (int i = start; i <= end; i++) {
                    System.out.println("    打印: " + i);
                }
            } else {
                int middle = (start + end) / 2;
                invokeAll(new PrintTask(start, middle), new PrintTask(middle + 1, end));
            }
        }
    }

    // 5. 对比
    private static void comparison() {
        System.out.println("  ForkJoinPool:");
        System.out.println("    ✓ 工作窃取算法，减少线程竞争");
        System.out.println("    ✓ 适合CPU密集型、分治任务");
        System.out.println("    ✓ 动态调整并行度");
        System.out.println("    ✓ 默认线程数 = CPU核心数");
        System.out.println();
        System.out.println("  普通ThreadPoolExecutor:");
        System.out.println("    ✓ 任务在共享队列中");
        System.out.println("    ✓ 适合IO密集型、异步任务");
        System.out.println("    ✓ 固定线程数");
        System.out.println("    ✓ 支持多种拒绝策略");
        System.out.println();
        System.out.println("  选择建议:");
        System.out.println("    - 分治算法（归并、快排等）: ForkJoinPool");
        System.out.println("    - CPU密集型大任务: ForkJoinPool");
        System.out.println("    - IO密集型: ThreadPoolExecutor");
        System.out.println("    - 异步任务: ThreadPoolExecutor");
    }

    // 6. 最佳实践
    private static void bestPractices() {
        System.out.println("  1. 阈值选择:");
        System.out.println("    - 太小: 任务创建/调度开销大");
        System.out.println("    - 太大: 无法充分利用并行");
        System.out.println("    - 建议: 1000-10000之间，根据实际情况调整");
        System.out.println();
        System.out.println("  2. 任务分解:");
        System.out.println("    - fork()后立即join()不如直接compute()高效");
        System.out.println("    - 使用invokeAll()同时fork多个子任务");
        System.out.println("    - 先执行一边任务，再fork另一边（减少等待）");
        System.out.println();
        System.out.println("  3. 使用parallelStream():");
        System.out.println("    - 简单场景推荐使用parallelStream()");
        System.out.println("    - 复杂场景使用自定义ForkJoinTask");
        System.out.println("    - 可以指定自定义ForkJoinPool");
        System.out.println();
        System.out.println("  4. 资源管理:");
        System.out.println("    - 不要嵌套使用多个ForkJoinPool");
        System.out.println("    - 复用CommonForkJoinPool");
        System.out.println("    - 注意避免递归过深导致栈溢出");
    }
}
