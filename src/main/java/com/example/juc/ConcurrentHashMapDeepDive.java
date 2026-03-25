package com.example.juc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ConcurrentHashMap深度剖析
 * 学习其内部实现原理和线程安全机制
 */
public class ConcurrentHashMapDeepDive {

    public static void main(String[] args) {
        System.out.println("=== ConcurrentHashMap深度剖析 ===");

        // 1. 基本使用
        System.out.println("\n1. 基本使用:");
        ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

        // put操作 - 线程安全
        map.put("key1", 1);
        map.put("key2", 2);
        map.put("key3", 3);

        System.out.println("初始map: " + map);

        // 2. 原子操作方法
        System.out.println("\n2. 原子操作方法:");

        // putIfAbsent - 如果key不存在则put
        Integer oldValue = map.putIfAbsent("key1", 100);
        System.out.println("putIfAbsent key1 (已存在): " + oldValue + ", map: " + map);

        Integer newValue = map.putIfAbsent("key4", 4);
        System.out.println("putIfAbsent key4 (新key): " + newValue + ", map: " + map);

        // computeIfAbsent - 计算不存在时的值
        Integer computed = map.computeIfAbsent("key5", k -> k.length());
        System.out.println("computeIfAbsent key5: " + computed + ", map: " + map);

        // computeIfPresent - 计算存在时的值
        Integer recomputed = map.computeIfPresent("key5", (k, v) -> v * 2);
        System.out.println("computeIfPresent key5: " + recomputed + ", map: " + map);

        // merge - 合并操作
        Integer merged = map.merge("key1", 10, (oldVal, newVal) -> oldVal + newVal);
        System.out.println("merge key1: " + merged + ", map: " + map);

        // 3. 并发性能测试
        System.out.println("\n3. 并发性能测试:");
        ConcurrentHashMap<Integer, AtomicInteger> counter = new ConcurrentHashMap<>();

        Thread[] threads = new Thread[10];
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threads.length; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    int key = j % 100; // 100个不同的key
                    counter.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
                }
            });
        }

        for (Thread t : threads) t.start();
        try {
            for (Thread t : threads) t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("并发操作耗时: " + (endTime - startTime) + "ms");
        System.out.println("最终counter大小: " + counter.size());

        // 验证结果正确性
        int totalCount = counter.values().stream().mapToInt(AtomicInteger::get).sum();
        System.out.println("总计数: " + totalCount + " (应为: " + (10 * 1000) + ")");

        // 4. 遍历操作
        System.out.println("\n4. 遍历操作:");

        // forEach - 普通遍历
        System.out.println("forEach遍历:");
        map.forEach((k, v) -> System.out.println("  " + k + " -> " + v));

        // forEach (并行) - 并行遍历
        System.out.println("forEach (并行):");
        map.forEach(2, (k, v) -> {
            System.out.println("  " + Thread.currentThread().getName() + ": " + k + " -> " + v);
        });

        // 5. 搜索操作
        System.out.println("\n5. 搜索操作:");

        // 搜索value > 5的entry
        Map.Entry<String, Integer> result = map.search(2, (k, v) -> v > 5 ? Map.entry(k, v) : null);
        System.out.println("搜索结果 (value > 5): " + result);

        // 6. reduce操作
        System.out.println("\n6. reduce操作:");

        // 求和
        Integer sum = map.reduce(2, (k, v) -> v, (v1, v2) -> v1 + v2);
        System.out.println("所有value的和: " + sum);

        // 7. 容量和扩容
        System.out.println("\n7. 容量和扩容:");
        ConcurrentHashMap<String, Integer> sizedMap = new ConcurrentHashMap<>(16, 0.75f, 2);
        System.out.println("创建时指定初始容量16, 负载因子0.75, 并发级别2");

        for (int i = 0; i < 20; i++) {
            sizedMap.put("key" + i, i);
        }
        System.out.println("插入20个元素后: " + sizedMap.size());

        // 8. key和value的集合视图
        System.out.println("\n8. 集合视图:");
        Set<String> keys = map.keySet();
        System.out.println("keySet: " + keys);

        // keySet是线程安全的视图
        keys.removeIf(key -> key.startsWith("key1"));
        System.out.println("删除key1*后: " + map);

        // 9. 与synchronized Map的性能对比概念演示
        System.out.println("\n9. 线程安全对比概念:");
        System.out.println("ConcurrentHashMap特点:");
        System.out.println("  - 分段锁/CAS操作");
        System.out.println("  - 读操作完全无锁");
        System.out.println("  - 写操作只锁部分数据");
        System.out.println("  - 支持高并发");
        System.out.println("\n相比synchronized HashMap:");
        System.out.println("  - 整个map加锁");
        System.out.println("  - 读写都阻塞");
        System.out.println("  - 并发度低");
    }
}