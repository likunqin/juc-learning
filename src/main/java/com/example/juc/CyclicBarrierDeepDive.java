package com.example.juc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CyclicBarrier 深度剖析示例
 * <p>
 * CyclicBarrier 是一个同步辅助工具，允许一组线程互相等待，直到所有线程都到达某个公共屏障点。
 * </p>
 *
 * <h3>核心特性：</h3>
 * <ul>
 *   <li>计数器可以重置（循环使用）</li>
 *   <li>支持到达屏障后的回调操作</li>
 *   <li>支持超时和中断</li>
 *   <li>线程可重复使用同一屏障</li>
 * </ul>
 *
 * <h3>适用场景：</h3>
 * <ul>
 *   <li>多阶段并行数据处理</li>
 *   <li>多线程协作完成复杂任务</li>
 *   <li>需要重用的同步点</li>
 *   <li>游戏中的回合制同步</li>
 * </ul>
 *
 * <h3>与 CountDownLatch 的区别：</h3>
 * <ul>
 *   <li>CyclicBarrier 可重用，CountDownLatch 不可重用</li>
 *   <li>CyclicBarrier 是线程间互相等待，CountDownLatch 是等待任务完成</li>
 *   <li>CyclicBarrier 支持回调，CountDownLatch 不支持</li>
 * </ul>
 */
public class CyclicBarrierDeepDive {

    /**
     * 场景1：多阶段数据处理
     * <p>
     * 典型应用：ETL（提取-转换-加载）流程的多阶段处理
     * </p>
     */
    public static void multiStageDataProcessing() throws InterruptedException {
        System.out.println("=== 场景1：多阶段数据处理 ===");

        // 创建一个屏障，5个线程到达时触发，并有回调
        CyclicBarrier barrier = new CyclicBarrier(5, () -> {
            System.out.println(">>> 所有线程到达，开始下一阶段 <<<");
        });

        ExecutorService executor = Executors.newFixedThreadPool(5);
        AtomicInteger stage = new AtomicInteger(1);

        for (int i = 0; i < 5; i++) {
            final int workerId = i;
            executor.submit(() -> {
                try {
                    // 阶段1：数据提取
                    int currentStage = stage.get();
                    Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
                    System.out.println("Worker " + workerId + " 阶段" + currentStage + "(提取) 完成");
                    barrier.await();

                    // 阶段2：数据转换
                    Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
                    System.out.println("Worker " + workerId + " 阶段" + currentStage + "(转换) 完成");
                    barrier.await();

                    // 阶段3：数据加载
                    Thread.sleep(ThreadLocalRandom.current().nextInt(100, 500));
                    System.out.println("Worker " + workerId + " 阶段" + currentStage + "(加载) 完成");
                    barrier.await();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        Thread.sleep(3000);
        executor.shutdown();
    }

    /**
     * 场景2：屏障的重用特性
     * <p>
     * 演示 CyclicBarrier 可以被多次使用
     * </p>
     */
    public static void barrierReuse() throws InterruptedException {
        System.out.println("\n=== 场景2：屏障重用 ===");

        int parties = 3;
        CyclicBarrier barrier = new CyclicBarrier(parties, () -> {
            System.out.println(">>> 屏障触发！所有参与者已到达 <<<");
        });

        ExecutorService executor = Executors.newFixedThreadPool(parties);

        // 第一轮使用
        System.out.println("--- 第一轮 ---");
        for (int i = 0; i < parties; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(100, 300));
                    System.out.println("线程 " + id + " 到达第一轮屏障");
                    barrier.await();
                    System.out.println("线程 " + id + " 通过第一轮屏障");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        Thread.sleep(1500);

        // 第二轮使用（重用同一个屏障）
        System.out.println("\n--- 第二轮（重用） ---");
        for (int i = 0; i < parties; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(100, 300));
                    System.out.println("线程 " + id + " 到达第二轮屏障");
                    barrier.await();
                    System.out.println("线程 " + id + " 通过第二轮屏障");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        Thread.sleep(1500);
        executor.shutdown();
    }

    /**
     * 场景3：超时等待
     * <p>
     * 演示 await 带超时的使用
     * </p>
     */
    public static void timeoutAwait() throws InterruptedException {
        System.out.println("\n=== 场景3：超时等待 ===");

        CyclicBarrier barrier = new CyclicBarrier(4);
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // 只提交3个线程，需要4个才能触发屏障
        for (int i = 0; i < 3; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(id * 200);
                    System.out.println("线程 " + id + " 到达屏障，等待其他线程...");

                    // 等待最多2秒
                    try {
                        barrier.await(2, TimeUnit.SECONDS);
                        System.out.println("线程 " + id + " 通过屏障");
                    } catch (TimeoutException e) {
                        System.out.println("线程 " + id + " 等待超时！");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(3000);
        executor.shutdown();
    }

    /**
     * 场景4：Barrier 的 reset() 使用
     * <p>
     * reset() 会重置屏障到初始状态，可能导致等待的线程抛出 BrokenBarrierException
     * </p>
     */
    public static void barrierReset() throws InterruptedException {
        System.out.println("\n=== 场景4：Barrier Reset ===");

        CyclicBarrier barrier = new CyclicBarrier(3);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // 提交2个线程，等待第3个线程
        for (int i = 0; i < 2; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    System.out.println("线程 " + id + " 到达屏障，等待...");
                    barrier.await();
                    System.out.println("线程 " + id + " 通过屏障");
                } catch (InterruptedException e) {
                    System.out.println("线程 " + id + " 被中断");
                    Thread.currentThread().interrupt();
                } catch (BrokenBarrierException e) {
                    System.out.println("线程 " + id + " 检测到屏障被破坏！");
                }
            });
        }

        Thread.sleep(500);

        // 主线程重置屏障
        System.out.println("主线程调用 reset() 重置屏障");
        barrier.reset();

        Thread.sleep(1000);
        executor.shutdown();
    }

    /**
     * 场景5：获取屏障状态
     * <p>
     * 演示如何查询屏障的当前状态
     * </p>
     */
    public static void barrierStatus() throws InterruptedException {
        System.out.println("\n=== 场景5：屏障状态查询 ===");

        CyclicBarrier barrier = new CyclicBarrier(3);

        System.out.println("初始状态:");
        printBarrierStatus(barrier);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        CountDownLatch doneLatch = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(200 + id * 100);
                    System.out.println("线程 " + id + " 到达，状态: " +
                            (3 - barrier.getNumberWaiting()) + " 未到达");
                    barrier.await();
                    System.out.println("线程 " + id + " 通过");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        doneLatch.await();
        System.out.println("\n所有线程通过后状态:");
        printBarrierStatus(barrier);

        executor.shutdown();
    }

    private static void printBarrierStatus(CyclicBarrier barrier) {
        System.out.println("  需要的参与者数: " + barrier.getParties());
        System.out.println("  正在等待的线程数: " + barrier.getNumberWaiting());
        System.out.println("  屏障是否被破坏: " + barrier.isBroken());
    }

    /**
     * 场景6：模拟游戏中的回合制
     * <p>
     * 典型应用：多个玩家在每个回合结束时同步
     * </p>
     */
    public static void gameTurnSynchronization() throws InterruptedException {
        System.out.println("\n=== 场景6：游戏回合同步 ===");

        String[] players = {"玩家A", "玩家B", "玩家C", "玩家D"};
        CyclicBarrier barrier = new CyclicBarrier(players.length, () -> {
            System.out.println("\n>>> 所有玩家回合结束，开始下一回合 <<<\n");
        });

        ExecutorService executor = Executors.newFixedThreadPool(players.length);

        // 模拟3个回合
        for (int round = 1; round <= 3; round++) {
            System.out.println("=== 第 " + round + " 回合 ===");
            final int currentRound = round;

            for (String player : players) {
                executor.submit(() -> {
                    try {
                        int actionTime = ThreadLocalRandom.current().nextInt(200, 800);
                        Thread.sleep(actionTime);
                        System.out.println(player + " 回合" + currentRound + " 操作完成 (" + actionTime + "ms)");
                        barrier.await();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            Thread.sleep(1500);
        }

        executor.shutdown();
    }

    /**
     * 场景7：带回调的并行计算
     * <p>
     * 使用回调在屏障触发时执行额外操作
     * </p>
     */
    public static void parallelWithCallback() throws InterruptedException {
        System.out.println("\n=== 场景7：带回调的并行计算 ===");

        final int[] partialResults = new int[3];
        CyclicBarrier barrier = new CyclicBarrier(3, () -> {
            // 屏障触发时的回调：汇总所有部分结果
            int sum = 0;
            for (int result : partialResults) {
                sum += result;
            }
            System.out.println(">>> 回调：汇总结果 = " + sum + " <<<");
        });

        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (int i = 0; i < 3; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    int result = ThreadLocalRandom.current().nextInt(10, 100);
                    partialResults[index] = result;
                    System.out.println("线程 " + index + " 计算完成，结果: " + result);
                    barrier.await();
                    System.out.println("线程 " + index + " 继续后续工作");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        Thread.sleep(2000);
        executor.shutdown();
    }

    /**
     * 最佳实践与注意事项
     */
    public static void bestPractices() {
        System.out.println("\n=== 最佳实践与注意事项 ===");

        System.out.println("""
        1. 区分 CyclicBarrier 和 CountDownLatch
           - CyclicBarrier: 可重用，用于多阶段同步
           - CountDownLatch: 一次性，用于等待任务完成

        2. 小心使用 reset()
           - reset() 会导致等待的线程抛出 BrokenBarrierException
           - 应在所有线程都通过屏障后再调用

        3. 处理 BrokenBarrierException
           - 屏障损坏时应该清理并退出
           - 可以创建新的屏障或终止任务

        4. await() 建议设置超时
           - 防止某个线程故障导致其他线程永久等待
           - 超时后可以根据业务逻辑决定是否重试

        5. 回调操作要快速
           - 回调在到达屏障的线程中执行
           - 耗时回调会影响所有等待线程的性能

        6. 线程数要匹配
           - 确保参与 await() 的线程数等于 parties
           - 否则可能导致永久等待

        7. 正确处理中断
           - 中断会导致屏障损坏
           - 被中断的线程应该清理资源
        """);
    }

    public static void main(String[] args) throws InterruptedException {
        multiStageDataProcessing();
        barrierReuse();
        timeoutAwait();
        barrierReset();
        barrierStatus();
        gameTurnSynchronization();
        parallelWithCallback();
        bestPractices();
    }
}
