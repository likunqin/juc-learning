package com.example.juc;

import java.util.concurrent.*;

/**
 * CountDownLatch 深度剖析示例
 * <p>
 * CountDownLatch 是一个同步辅助工具，允许一个或多个线程等待其他线程完成一组操作。
 * </p>
 *
 * <h3>核心特性：</h3>
 * <ul>
 *   <li>计数器只能减少，不能增加</li>
 *   <li>计数器归零后，await() 方法立即返回</li>
 *   <li>一旦归零，不能重置（不可重复使用）</li>
 * </ul>
 *
 * <h3>适用场景：</h3>
 * <ul>
 *   <li>并行任务执行后结果聚合</li>
 *   <li>等待多个服务初始化完成</li>
 *   <li>并发测试中的多线程协调</li>
 *   <li>主线程等待多个子任务完成</li>
 * </ul>
 */
public class CountDownLatchDeepDive {

    /**
     * 场景1：并行执行多个任务，等待所有完成后聚合结果
     * <p>
     * 典型应用：从多个数据源并行获取数据后汇总
     * </p>
     */
    public static void parallelTaskAggregation() throws InterruptedException {
        System.out.println("=== 场景1：并行任务聚合 ===");

        // 创建3个任务，主线程等待所有任务完成
        CountDownLatch latch = new CountDownLatch(3);
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // 用于收集结果的线程安全容器
        ConcurrentMap<String, String> results = new ConcurrentHashMap<>();

        // 提交3个并行任务
        executor.submit(() -> {
            try {
                Thread.sleep(1000); // 模拟耗时操作
                results.put("user", "用户数据");
                System.out.println("获取用户数据完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                Thread.sleep(800);
                results.put("order", "订单数据");
                System.out.println("获取订单数据完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                Thread.sleep(1200);
                results.put("product", "商品数据");
                System.out.println("获取商品数据完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        // 主线程等待所有任务完成
        latch.await();
        executor.shutdown();

        // 聚合结果
        System.out.println("所有任务完成，结果汇总: " + results);
    }

    /**
     * 场景2：等待多个服务初始化完成
     * <p>
     * 典型应用：应用启动时等待多个依赖服务就绪
     * </p>
     */
    public static void serviceInitialization() throws InterruptedException {
        System.out.println("\n=== 场景2：服务初始化 ===");

        String[] services = {"数据库", "缓存", "消息队列", "配置中心"};
        CountDownLatch latch = new CountDownLatch(services.length);

        for (String service : services) {
            new Thread(() -> {
                try {
                    System.out.println(service + " 初始化中...");
                    Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1500));
                    System.out.println(service + " 初始化完成 ✓");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
        System.out.println("所有服务初始化完成，应用可以启动！");
    }

    /**
     * 场景3：竞赛场景 - 第一个完成的执行后续操作
     * <p>
     * 通过设置 count=1，让第一个完成任务的线程触发后续操作
     * </p>
     */
    public static void raceCondition() throws InterruptedException {
        System.out.println("\n=== 场景3：竞赛场景 ===");

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean hasWinner = new AtomicBoolean(false);
        ExecutorService executor = Executors.newFixedThreadPool(5);

        String winner = null;

        // 创建5个线程竞赛
        for (int i = 0; i < 5; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    // 随机完成时间
                    int time = ThreadLocalRandom.current().nextInt(500, 2000);
                    Thread.sleep(time);

                    // 尝试成为赢家
                    if (hasWinner.compareAndSet(false, true)) {
                        System.out.println("线程 " + id + " 第一个完成！耗时: " + time + "ms");
                        latch.countDown(); // 通知主线程
                    } else {
                        System.out.println("线程 " + id + " 完成了，但已经有赢家了");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("竞赛结束，其他任务可以取消...");
    }

    /**
     * 场景4：多阶段并行处理
     * <p>
     * 注意：CountDownLatch 不可重用，多阶段需要创建多个实例
     * 如果需要重用，应使用 CyclicBarrier
     * </p>
     */
    public static void multiStageProcessing() throws InterruptedException {
        System.out.println("\n=== 场景4：多阶段处理（使用多个Latch）===");

        int stageCount = 3;
        for (int stage = 1; stage <= stageCount; stage++) {
            System.out.println("\n阶段 " + stage + " 开始");
            CountDownLatch stageLatch = new CountDownLatch(3);

            for (int i = 0; i < 3; i++) {
                final int taskId = i;
                new Thread(() -> {
                    try {
                        int workTime = ThreadLocalRandom.current().nextInt(200, 800);
                        Thread.sleep(workTime);
                        System.out.println("  任务 " + taskId + " 阶段" + stage + " 完成 (" + workTime + "ms)");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        stageLatch.countDown();
                    }
                }).start();
            }

            stageLatch.await();
            System.out.println("阶段 " + stage + " 所有任务完成");
        }
    }

    /**
     * 场景5：await 带超时
     * <p>
     * 在指定时间内等待，超时后不阻塞
     * </p>
     */
    public static void awaitWithTimeout() throws InterruptedException {
        System.out.println("\n=== 场景5：await 带超时 ===");

        CountDownLatch latch = new CountDownLatch(5);
        ExecutorService executor = Executors.newFixedThreadPool(3);

        // 提交5个任务，但只有3个线程
        for (int i = 0; i < 5; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(1000); // 每个任务需要1秒
                    System.out.println("任务 " + id + " 完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待最多2秒
        long start = System.currentTimeMillis();
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("等待结果: " + (completed ? "全部完成" : "超时"));
        System.out.println("等待时间: " + elapsed + "ms");
        System.out.println("剩余计数: " + latch.getCount());

        executor.shutdownNow();
    }

    /**
     * 场景6：批量并行计算
     * <p>
     * 将大任务拆分为多个小任务并行执行
     * </p>
     */
    public static void parallelCalculation() throws InterruptedException {
        System.out.println("\n=== 场景6：批量并行计算 ===");

        int total = 100;
        int batchSize = 10;
        int taskCount = (total + batchSize - 1) / batchSize;

        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicLong sum = new AtomicLong(0);
        ExecutorService executor = Executors.newFixedThreadPool(4);

        // 将1-100的求和拆分为10个批次
        for (int i = 0; i < taskCount; i++) {
            final int start = i * batchSize + 1;
            final int end = Math.min(start + batchSize - 1, total);

            executor.submit(() -> {
                try {
                    int batchSum = 0;
                    for (int j = start; j <= end; j++) {
                        batchSum += j;
                        Thread.sleep(10); // 模拟计算耗时
                    }
                    sum.addAndGet(batchSum);
                    System.out.printf("批次 [%d-%d] 求和: %d\n", start, end, batchSum);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        System.out.println("最终求和结果: " + sum.get());
        System.out.println("验证（1+2+...+100）: " + (total * (total + 1) / 2));
    }

    /**
     * 最佳实践与注意事项
     */
    public static void bestPractices() {
        System.out.println("\n=== 最佳实践与注意事项 ===");

        System.out.println("""
        1. countDown() 必须在 finally 块中调用
           - 确保计数器一定会递减
           - 避免任务异常导致 latch 永远无法归零

        2. await() 建议设置超时
           - 防止某个任务异常导致主线程永久阻塞
           - 可以根据业务需要决定是否继续等待

        3. CountDownLatch 不可重用
           - 计数器归零后无法重置
           - 如需重用请使用 CyclicBarrier

        4. 避免在多个 latch 间循环等待
           - 容易造成死锁
           - 注意任务提交顺序和等待顺序

        5. 大量任务时考虑使用线程池
           - 避免创建过多线程
           - 控制并发度，提高资源利用率
        """);
    }

    public static void main(String[] args) throws InterruptedException {
        parallelTaskAggregation();
        serviceInitialization();
        raceCondition();
        multiStageProcessing();
        awaitWithTimeout();
        parallelCalculation();
        bestPractices();
    }
}
