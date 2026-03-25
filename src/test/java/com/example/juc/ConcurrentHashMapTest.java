package com.example.juc;

import org.junit.jupiter.api.Test;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConcurrentHashMap 单元测试
 */
class ConcurrentHashMapTest {

    @Test
    void testConcurrentHashMapThreadSafety() throws InterruptedException {
        Map<Integer, Integer> map = new ConcurrentHashMap<>();
        int threads = 10;
        int operationsPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threads);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        int key = threadId * operationsPerThread + j;
                        map.put(key, j);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(threads * operationsPerThread, map.size(),
            "ConcurrentHashMap 应该正确存储所有条目");
    }

    @Test
    void testConcurrentHashMapVsHashMapConcurrentModification() {
        // HashMap 并发修改会抛出异常
        Map<Integer, Integer> hashMap = new HashMap<>();
        hashMap.put(1, 1);
        hashMap.put(2, 2);
        hashMap.put(3, 3);

        // ConcurrentHashMap 允许并发迭代
        Map<Integer, Integer> concurrentMap = new ConcurrentHashMap<>();
        concurrentMap.put(1, 1);
        concurrentMap.put(2, 2);
        concurrentMap.put(3, 3);

        // HashMap 遍历时修改会抛出 ConcurrentModificationException
        assertThrows(ConcurrentModificationException.class, () -> {
            for (Map.Entry<Integer, Integer> entry : hashMap.entrySet()) {
                if (entry.getKey() == 2) {
                    hashMap.put(4, 4); // 修改时遍历
                }
            }
        });

        // ConcurrentHashMap 不会抛出异常
        assertDoesNotThrow(() -> {
            for (Map.Entry<Integer, Integer> entry : concurrentMap.entrySet()) {
                if (entry.getKey() == 2) {
                    concurrentMap.put(4, 4);
                }
            }
        });
    }

    @Test
    void testConcurrentHashMapPutIfAbsent() {
        Map<String, String> map = new ConcurrentHashMap<>();

        // 第一次 putIfAbsent 应该成功
        assertNull(map.putIfAbsent("key1", "value1"));
        assertEquals("value1", map.get("key1"));

        // 第二次 putIfAbsent 应该返回现有值
        assertEquals("value1", map.putIfAbsent("key1", "value2"));
        assertEquals("value1", map.get("key1"), "值不应被覆盖");
    }

    @Test
    void testConcurrentHashMapComputeIfAbsent() {
        Map<String, Integer> map = new ConcurrentHashMap<>();

        // 第一次 computeIfAbsent 应该执行计算
        int result1 = map.computeIfAbsent("key1", k -> k.length());
        assertEquals(4, result1);
        assertEquals(4, map.get("key1"));

        // 第二次 computeIfAbsent 不应重新计算
        int result2 = map.computeIfAbsent("key1", k -> {
            throw new RuntimeException("不应执行");
        });
        assertEquals(4, result2);
    }

    @Test
    void testConcurrentHashMapMerge() {
        Map<String, Integer> map = new ConcurrentHashMap<>();
        map.put("key1", 5);

        // 第一次 merge: key 不存在
        int result1 = map.merge("key2", 3, Integer::sum);
        assertEquals(3, result1);
        assertEquals(3, map.get("key2"));

        // 第二次 merge: key 已存在
        int result2 = map.merge("key1", 2, Integer::sum);
        assertEquals(7, result2);
        assertEquals(7, map.get("key1"));
    }
}
