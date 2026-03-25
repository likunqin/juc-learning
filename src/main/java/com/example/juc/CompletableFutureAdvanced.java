package com.example.juc;

import java.util.concurrent.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CompletableFuture进阶学习示例
 * 复杂的异步编程场景和组合操作
 */
public class CompletableFutureAdvanced {

    public static void main(String[] args) throws Exception {
        System.out.println("=== CompletableFuture进阶学习示例 ===\n");

        // 1. 多个Future组合
        System.out.println("1. 多个Future组合:");
        combineMultiple();

        // 2. 异常处理进阶
        System.out.println("\n2. 异常处理进阶:");
        advancedExceptionHandling();

        // 3. 超时控制
        System.out.println("\n3. 超时控制:");
        timeoutHandling();

        // 4. 批量处理
        System.out.println("\n4. 批量异步处理:");
        batchProcessing();

        // 5. 条件执行
        System.out.println("\n5. 条件执行:");
        conditionalExecution();

        // 6. 轮询与重试
        System.out.println("\n6. 轮询与重试:");
        retryPattern();

        // 7. 背压控制
        System.out.println("\n7. 并发度控制（背压）:");
        backpressureControl();

        // 8. 实际场景 - 电商订单处理
        System.out.println("\n8. 实际场景 - 电商订单处理:");
        orderProcessing();
    }

    // 1. 多个Future组合
    private static void combineMultiple() throws Exception {
        // 场景: 获取用户、订单、支付信息
        CompletableFuture<String> user = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return "User:张三";
        });

        CompletableFuture<String> order = CompletableFuture.supplyAsync(() -> {
            sleep(300);
            return "Order:订单001";
        });

        CompletableFuture<String> payment = CompletableFuture.supplyAsync(() -> {
            sleep(250);
            return "Payment:已支付";
        });

        // 等待所有完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(user, order, payment);
        allOf.join();

        System.out.println("  所有信息: " + user.get() + ", " + order.get() + ", " + payment.get());

        // 获取最快完成的结果
        CompletableFuture<String> fast = CompletableFuture.anyOf(user, order, payment)
            .thenApply(o -> o.toString());
        System.out.println("  最快完成: " + fast.get());
    }

    // 2. 异常处理进阶
    private static void advancedExceptionHandling() {
        // 处理链中的异常
        CompletableFuture.supplyAsync(() -> {
            if (Math.random() > 0.5) {
                throw new RuntimeException("随机异常");
            }
            return "成功";
        })
        .thenApply(s -> s.toUpperCase())
        .exceptionally(ex -> {
            System.out.println("  捕获异常: " + ex.getMessage());
            return "默认值";
        })
        .thenAccept(result -> System.out.println("  最终结果: " + result))
        .join();

        // handle - 处理成功和失败
        CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("模拟失败");
        })
        .handle((result, ex) -> {
            if (ex != null) {
                return "异常处理: " + ex.getMessage();
            }
            return "成功处理: " + result;
        })
        .thenAccept(System.out::println)
        .join();

        // whenComplete - 完成后的回调
        CompletableFuture.supplyAsync(() -> "测试数据")
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    System.out.println("  成功完成: " + result);
                }
            })
            .join();
    }

    // 3. 超时控制
    private static void timeoutHandling() {
        // orTimeout - 超时后抛出异常
        CompletableFuture<String> withTimeout = CompletableFuture.supplyAsync(() -> {
            sleep(2000);
            return "完成";
        })
        .orTimeout(1, TimeUnit.SECONDS)
        .exceptionally(ex -> {
            if (ex instanceof TimeoutException) {
                return "超时返回默认值";
            }
            return "其他异常: " + ex.getMessage();
        });

        System.out.println("  超时测试: " + withTimeout.join());

        // completeOnTimeout - 超时后使用默认值
        CompletableFuture<String> completeOnTimeout = CompletableFuture.supplyAsync(() -> {
            sleep(2000);
            return "完成";
        })
        .completeOnTimeout("超时默认值", 1, TimeUnit.SECONDS);

        System.out.println("  completeOnTimeout: " + completeOnTimeout.join());
    }

    // 4. 批量处理
    private static void batchProcessing() throws Exception {
        List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);

        // 方式1: 并行处理所有ID
        List<CompletableFuture<String>> futures = ids.stream()
            .map(id -> CompletableFuture.supplyAsync(() -> fetchData(id)))
            .collect(Collectors.toList());

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0]));

        // 等待所有完成并收集结果
        List<String> results = allFutures.thenApply(v ->
            futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList())
        ).get();

        System.out.println("  批量处理结果: " + results);

        // 方式2: 控制并发度的批量处理
        int concurrency = 2;
        Semaphore semaphore = new Semaphore(concurrency);

        List<CompletableFuture<String>> limitedFutures = ids.stream()
            .map(id -> CompletableFuture.supplyAsync(() -> {
                try {
                    semaphore.acquire();
                    return fetchData(id);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "中断";
                } finally {
                    semaphore.release();
                }
            }))
            .collect(Collectors.toList());

        CompletableFuture.allOf(limitedFutures.toArray(new CompletableFuture[0]))
            .thenAccept(v -> System.out.println("  限流批量处理完成"))
            .join();
    }

    private static String fetchData(int id) {
        sleep(200 + id * 50);
        return "Data-" + id;
    }

    // 5. 条件执行
    private static void conditionalExecution() {
        boolean condition = true;

        CompletableFuture<String> future = CompletableFuture.completedFuture("初始值")
            .thenCompose(value -> {
                if (condition) {
                    return CompletableFuture.supplyAsync(() -> {
                        sleep(300);
                        return value + " -> 处理A";
                    });
                } else {
                    return CompletableFuture.supplyAsync(() -> {
                        sleep(300);
                        return value + " -> 处理B";
                    });
                }
            });

        System.out.println("  条件执行结果: " + future.join());
    }

    // 6. 轮询与重试
    private static void retryPattern() {
        retry(() -> {
            if (Math.random() < 0.7) {
                throw new RuntimeException("失败");
            }
            return "成功";
        }, 5)
        .thenAccept(result -> System.out.println("  重试结果: " + result))
        .join();
    }

    // 重试工具方法
    private static <T> CompletableFuture<T> retry(Supplier<T> supplier, int maxRetries) {
        CompletableFuture<T> result = new CompletableFuture<>();
        retryInternal(supplier, maxRetries, result, 0);
        return result;
    }

    private static <T> void retryInternal(Supplier<T> supplier, int maxRetries,
                                          CompletableFuture<T> result, int attempt) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })
            .whenComplete((value, ex) -> {
                if (ex == null) {
                    result.complete(value);
                } else if (attempt < maxRetries) {
                    System.out.println("  第" + (attempt + 1) + "次失败，重试...");
                    sleep(500);
                    retryInternal(supplier, maxRetries, result, attempt + 1);
                } else {
                    result.completeExceptionally(ex);
                }
            });
    }

    @FunctionalInterface
    interface Supplier<T> {
        T get() throws Exception;
    }

    // 7. 背压控制
    private static void backpressureControl() {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        List<String> tasks = Arrays.asList("任务1", "任务2", "任务3", "任务4", "任务5");

        List<CompletableFuture<String>> futures = new ArrayList<>();

        for (String task : tasks) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                System.out.println("  开始: " + task);
                sleep(500);
                System.out.println("  完成: " + task);
                return task + "-完成";
            }, executor);

            futures.add(future);

            // 控制提交速率
            sleep(200);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenAccept(v -> System.out.println("  所有任务完成"))
            .join();

        executor.shutdown();
    }

    // 8. 实际场景 - 电商订单处理
    private static void orderProcessing() throws Exception {
        long startTime = System.currentTimeMillis();

        // 步骤1: 获取订单基础信息
        CompletableFuture<Order> orderInfo = CompletableFuture.supplyAsync(() -> {
            sleep(200);
            return new Order("ORD-001", "张三", 99.99);
        });

        // 步骤2: 并行获取相关信息
        CompletableFuture<UserProfile> userProfile = orderInfo.thenComposeAsync(order ->
            CompletableFuture.supplyAsync(() -> {
                sleep(150);
                return new UserProfile(order.userId, "VIP", "北京市");
            })
        );

        CompletableFuture<Inventory> inventory = orderInfo.thenComposeAsync(order ->
            CompletableFuture.supplyAsync(() -> {
                sleep(100);
                return new Inventory("商品A", 100, true);
            })
        );

        CompletableFuture<Coupon> coupon = orderInfo.thenComposeAsync(order ->
            CompletableFuture.supplyAsync(() -> {
                sleep(180);
                return new Coupon("优惠10", 10.0);
            })
        );

        // 等待所有信息获取完成
        CompletableFuture<Void> allInfo = CompletableFuture.allOf(userProfile, inventory, coupon);

        // 步骤3: 计算最终价格
        CompletableFuture<OrderResult> result = allInfo.thenApplyAsync(v -> {
            Order order = orderInfo.join();
            UserProfile profile = userProfile.join();
            Inventory inv = inventory.join();
            Coupon c = coupon.join();

            double finalPrice = order.price;
            if (c != null) {
                finalPrice -= c.discount;
            }
            if ("VIP".equals(profile.level)) {
                finalPrice *= 0.95; // VIP 95折
            }

            return new OrderResult(order, profile, inv, c, finalPrice);
        });

        OrderResult finalResult = result.get();
        long endTime = System.currentTimeMillis();

        System.out.println("  === 订单处理结果 ===");
        System.out.println("  " + finalResult);
        System.out.println("  总耗时: " + (endTime - startTime) + "ms");
    }

    // 辅助类
    static class Order {
        final String orderId;
        final String userId;
        final double price;

        Order(String orderId, String userId, double price) {
            this.orderId = orderId;
            this.userId = userId;
            this.price = price;
        }
    }

    static class UserProfile {
        final String userId;
        final String level;
        final String address;

        UserProfile(String userId, String level, String address) {
            this.userId = userId;
            this.level = level;
            this.address = address;
        }
    }

    static class Inventory {
        final String productId;
        final int stock;
        final boolean available;

        Inventory(String productId, int stock, boolean available) {
            this.productId = productId;
            this.stock = stock;
            this.available = available;
        }
    }

    static class Coupon {
        final String name;
        final double discount;

        Coupon(String name, double discount) {
            this.name = name;
            this.discount = discount;
        }
    }

    static class OrderResult {
        final Order order;
        final UserProfile user;
        final Inventory inventory;
        final Coupon coupon;
        final double finalPrice;

        OrderResult(Order order, UserProfile user, Inventory inventory, Coupon coupon, double finalPrice) {
            this.order = order;
            this.user = user;
            this.inventory = inventory;
            this.coupon = coupon;
            this.finalPrice = finalPrice;
        }

        @Override
        public String toString() {
            return String.format("订单:%s, 用户:%s(%s), 库存:%d, 优惠:%s, 最终价格:%.2f",
                order.orderId, user.userId, user.level, inventory.stock,
                coupon != null ? coupon.name : "无", finalPrice);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
