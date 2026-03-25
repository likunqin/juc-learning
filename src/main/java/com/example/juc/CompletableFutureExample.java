package com.example.juc;

import java.util.concurrent.*;
import java.util.List;
import java.util.Arrays;
import java.util.Random;

/**
 * CompletableFuture学习示例
 * Java 8+ 异步编程的核心API
 */
public class CompletableFutureExample {

    private static final Random random = new Random();

    public static void main(String[] args) {
        System.out.println("=== CompletableFuture学习示例 ===\n");

        // 1. 基本异步任务
        System.out.println("1. 基本异步任务:");
        basicAsyncTask();

        // 2. 链式调用
        System.out.println("\n2. 链式调用:");
        chainingCalls();

        // 3. 组合多个Future
        System.out.println("\n3. 组合多个Future:");
        combiningFutures();

        // 4. 异常处理
        System.out.println("\n4. 异常处理:");
        exceptionHandling();

        // 5. 实际场景示例
        System.out.println("\n5. 实际场景 - 并行获取用户数据:");
        realWorldScenario();
    }

    // 1. 基本异步任务
    private static void basicAsyncTask() {
        // 使用ForkJoinPool作为默认线程池
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            sleep(1000);
            return "任务完成";
        });

        // 添加回调
        future.thenAccept(result -> System.out.println("回调执行: " + result));

        // 阻塞获取结果
        try {
            String result = future.get();
            System.out.println("主线程获取结果: " + result);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    // 2. 链式调用
    private static void chainingCalls() {
        CompletableFuture.supplyAsync(() -> {
            System.out.println("步骤1: 获取用户ID");
            return 123;
        })
        .thenApplyAsync(userId -> {
            System.out.println("步骤2: 查询用户信息 (ID: " + userId + ")");
            sleep(500);
            return "User-" + userId;
        })
        .thenApplyAsync(userName -> {
            System.out.println("步骤3: 获取用户权限 (User: " + userName + ")");
            sleep(500);
            return userName + " [ADMIN]";
        })
        .thenAccept(finalResult -> System.out.println("最终结果: " + finalResult))
        .join(); // 等待完成
    }

    // 3. 组合多个Future
    private static void combiningFutures() {
        // thenCombine - 组合两个Future的结果
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return 10;
        });

        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return 20;
        });

        CompletableFuture<Integer> combined = future1.thenCombine(future2, (a, b) -> {
            System.out.println("组合两个结果: " + a + " + " + b);
            return a + b;
        });

        System.out.println("组合结果: " + combined.join());

        // allOf - 等待所有Future完成
        System.out.println("\nallOf示例:");
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            CompletableFuture.runAsync(() -> { sleep(200); System.out.println("任务A完成"); }),
            CompletableFuture.runAsync(() -> { sleep(400); System.out.println("任务B完成"); }),
            CompletableFuture.runAsync(() -> { sleep(600); System.out.println("任务C完成"); })
        );

        allFutures.join();
        System.out.println("所有任务已完成");

        // anyOf - 等待任意一个Future完成
        System.out.println("\nanyOf示例:");
        CompletableFuture<Object> anyFuture = CompletableFuture.anyOf(
            CompletableFuture.supplyAsync(() -> { sleep(1000); return "慢任务"; }),
            CompletableFuture.supplyAsync(() -> { sleep(200); return "快任务"; }),
            CompletableFuture.supplyAsync(() -> { sleep(500); return "中等任务"; })
        );

        System.out.println("最先完成的任务: " + anyFuture.join());
    }

    // 4. 异常处理
    private static void exceptionHandling() {
        // exceptionally - 处理异常
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            if (random.nextBoolean()) {
                throw new RuntimeException("随机异常");
            }
            return "成功";
        }).exceptionally(ex -> {
            System.out.println("捕获异常: " + ex.getMessage());
            return "默认值";
        });

        System.out.println("结果1: " + future1.join());

        // handle - 无论成功或失败都执行
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("模拟异常");
        }).handle((result, ex) -> {
            if (ex != null) {
                System.out.println("handle处理异常: " + ex.getMessage());
                return "恢复后的值";
            }
            return String.valueOf(result);
        });

        System.out.println("结果2: " + future2.join());

        // whenComplete - 完成时的回调
        CompletableFuture.supplyAsync(() -> "测试")
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    System.out.println("whenComplete - 成功: " + result);
                }
            })
            .join();
    }

    // 5. 实际场景 - 并行获取用户数据
    private static void realWorldScenario() {
        long startTime = System.currentTimeMillis();

        // 模拟并行获取用户的不同数据
        CompletableFuture<String> userProfile = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return "用户: 张三";
        });

        CompletableFuture<List<String>> userOrders = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            return Arrays.asList("订单1", "订单2", "订单3");
        });

        CompletableFuture<Integer> userPoints = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return 1250;
        });

        // 组合所有结果
        CompletableFuture<UserData> combined = CompletableFuture.allOf(userProfile, userOrders, userPoints)
            .thenApply(v -> {
                try {
                    return new UserData(
                        userProfile.get(),
                        userOrders.get(),
                        userPoints.get()
                    );
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });

        UserData result = combined.join();
        long endTime = System.currentTimeMillis();

        System.out.println("=== 用户数据汇总 ===");
        System.out.println(result);
        System.out.println("总耗时: " + (endTime - startTime) + "ms");
        System.out.println("(并行执行，而非串行 " + (300 + 500 + 200) + "ms)");
    }

    // 辅助方法
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 用户数据DTO
    static class UserData {
        private final String profile;
        private final List<String> orders;
        private final int points;

        public UserData(String profile, List<String> orders, int points) {
            this.profile = profile;
            this.orders = orders;
            this.points = points;
        }

        @Override
        public String toString() {
            return "UserData{\n" +
                   "  profile='" + profile + "',\n" +
                   "  orders=" + orders + ",\n" +
                   "  points=" + points + "\n" +
                   '}';
        }
    }
}
