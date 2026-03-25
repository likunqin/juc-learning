package com.example.juc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CompletableFuture 单元测试
 */
class CompletableFutureExampleTest {

    @Test
    void testCompletableFutureBasic() throws InterruptedException, ExecutionException {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello");

        assertEquals("Hello", future.get(), "异步任务应该返回正确结果");
    }

    @Test
    void testCompletableFutureThenApply() throws InterruptedException, ExecutionException {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello")
                .thenApply(s -> s + " World")
                .thenApply(String::toUpperCase);

        assertEquals("HELLO WORLD", future.get(), "链式调用应该正确执行");
    }

    @Test
    void testCompletableFutureThenAccept() throws InterruptedException {
        AtomicInteger result = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(1);

        CompletableFuture.supplyAsync(() -> 42)
                .thenAccept(value -> {
                    result.set(value);
                    latch.countDown();
                });

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(42, result.get(), "thenAccept 应该正确消费结果");
    }

    @Test
    void testCompletableFutureAllOf() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(3);

        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        CompletableFuture<Void> future3 = CompletableFuture.runAsync(() -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2, future3);

        allFutures.join();
        assertEquals(3, counter.get(), "所有任务都应该完成");
    }

    @Test
    void testCompletableFutureExceptionally() throws InterruptedException, ExecutionException {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("测试异常");
        }).exceptionally(e -> {
            return "默认值: " + e.getMessage();
        });

        String result = future.get();
        assertTrue(result.startsWith("默认值"), "异常应该被捕获并返回默认值");
        assertTrue(result.contains("测试异常"), "异常信息应该被保留");
    }

    @Test
    void testCompletableFutureHandle() throws InterruptedException, ExecutionException {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> 10 / 0)
                .handle((result, ex) -> {
                    if (ex != null) {
                        return -1; // 异常时返回 -1
                    }
                    return result;
                });

        assertEquals(-1, future.get(), "异常应该被处理并返回替代值");
    }

    @Test
    void testCompletableFutureCombine() throws InterruptedException, ExecutionException {
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> 5);
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> 3);

        CompletableFuture<Integer> combined = future1.thenCombine(future2, (a, b) -> a * b);

        assertEquals(15, combined.get(), "两个 Future 的结果应该被正确组合");
    }

    @Test
    void testCompletableFutureThenCompose() throws InterruptedException, ExecutionException {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Hello")
                .thenCompose(s -> CompletableFuture.supplyAsync(() -> s + " World"));

        assertEquals("Hello World", future.get(), "thenCompose 应该正确链接异步操作");
    }

    @Test
    void testCompletableFutureTimeout() {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
                return "完成";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "中断";
            }
        });

        assertThrows(TimeoutException.class, () -> {
            future.get(500, TimeUnit.MILLISECONDS);
        }, "超时时应该抛出 TimeoutException");
    }

    @Test
    void testCompletableFutureCancel() {
        CompletableFuture<String> future = new CompletableFuture<>();

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(2000);
                future.complete("完成");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        thread.start();

        // 立即取消
        boolean cancelled = future.cancel(true);
        assertTrue(cancelled, "Future 应该被成功取消");
        assertTrue(future.isCancelled(), "Future 应该标记为已取消");
    }
}
