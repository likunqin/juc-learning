package com.example.juc;

import java.util.concurrent.*;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;

/**
 * ConcurrentSkipListMap学习示例
 * 基于跳表（SkipList）的线程安全有序Map
 */
public class ConcurrentSkipListMapExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ConcurrentSkipListMap学习示例 ===\n");

        // 1. 基本使用 - 有序性
        System.out.println("1. 基本使用 - 有序存储:");
        basicUsage();

        // 2. 导航方法
        System.out.println("\n2. 导航方法:");
        navigationMethods();

        // 3. 并发写入测试
        System.out.println("\n3. 并发写入测试:");
        concurrentWriteTest();

        // 4. 与ConcurrentHashMap对比
        System.out.println("\n4. ConcurrentSkipListMap vs ConcurrentHashMap:");
        comparison();

        // 5. ConcurrentSkipListSet示例
        System.out.println("\n5. ConcurrentSkipListSet - 有序Set:");
        skipListSetDemo();
    }

    // 1. 基本使用
    private static void basicUsage() {
        ConcurrentSkipListMap<String, Integer> map = new ConcurrentSkipListMap<>();

        // 添加元素（按key自然排序）
        map.put("banana", 3);
        map.put("apple", 1);
        map.put("orange", 5);
        map.put("grape", 2);
        map.put("cherry", 4);

        System.out.println("  按key排序的Map: " + map);
        System.out.println("  首个entry: " + map.firstEntry());
        System.out.println("  末尾entry: " + map.lastEntry());
    }

    // 2. 导航方法
    private static void navigationMethods() {
        ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<>();

        for (int i = 1; i <= 10; i++) {
            map.put(i, "Value" + i);
        }

        System.out.println("  Map内容: " + map.keySet());

        // 导航方法
        System.out.println("  floorEntry(5) - <=5的最大key: " + map.floorEntry(5));
        System.out.println("  ceilingEntry(5) - >=5的最小key: " + map.ceilingEntry(5));
        System.out.println("  lowerEntry(5) - <5的最大key: " + map.lowerEntry(5));
        System.out.println("  higherEntry(5) - >5的最小key: " + map.higherEntry(5));

        // 范围查询
        System.out.println("  subMap(3, 7) - [3,7): " + map.subMap(3, 7));
        System.out.println("  headMap(5) - <5: " + map.headMap(5));
        System.out.println("  tailMap(6) - >=6: " + map.tailMap(6));

        // 可逆遍历
        System.out.println("  descendingMap - 逆序:");
        NavigableMap<Integer, String> descending = map.descendingMap();
        descending.forEach((k, v) -> System.out.println("    " + k + " -> " + v));
    }

    // 3. 并发写入测试
    private static void concurrentWriteTest() throws InterruptedException {
        ConcurrentSkipListMap<Integer, String> map = new ConcurrentSkipListMap<>();

        Thread[] threads = new Thread[5];

        for (int i = 0; i < threads.length; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    int key = threadId * 100 + j;
                    map.put(key, "Thread" + threadId + "-Value" + j);
                }
            });
        }

        long startTime = System.currentTimeMillis();
        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();
        long endTime = System.currentTimeMillis();

        System.out.println("  并发插入500个元素，耗时: " + (endTime - startTime) + "ms");
        System.out.println("  Map大小: " + map.size());
        System.out.println("  是否保持有序: " + isOrdered(map));
    }

    // 检查Map是否有序
    private static <K extends Comparable<K>> boolean isOrdered(ConcurrentSkipListMap<K, ?> map) {
        K prev = null;
        for (K key : map.keySet()) {
            if (prev != null && prev.compareTo(key) > 0) {
                return false;
            }
            prev = key;
        }
        return true;
    }

    // 4. 与ConcurrentHashMap对比
    private static void comparison() {
        System.out.println("  ConcurrentSkipListMap:");
        System.out.println("    ✓ 基于SkipList（跳表）结构");
        System.out.println("    ✓ 所有操作时间复杂度 O(log n)");
        System.out.println("    ✓ key是有序的（自然排序或自定义Comparator）");
        System.out.println("    ✓ 支持丰富的导航方法（floor, ceiling等）");
        System.out.println("    ✓ 支持范围查询");
        System.out.println("    ✓ 内存占用较高（多层索引）");
        System.out.println("    ✓ 读操作无锁，写操作CAS+锁");

        System.out.println("\n  ConcurrentHashMap:");
        System.out.println("    ✓ 基于数组+链表/红黑树");
        System.out.println("    ✓ 大部分操作 O(1)，扩容时O(n)");
        System.out.println("    ✓ key无序");
        System.out.println("    ✓ 不支持导航方法");
        System.out.println("    ✓ 内存占用低");
        System.out.println("    ✓ 性能通常优于SkipListMap");

        System.out.println("\n  选择建议:");
        System.out.println("    - 需要有序/范围查询: ConcurrentSkipListMap");
        System.out.println("    - 只需要高性能: ConcurrentHashMap");
        System.out.println("    - 需要tail/head等操作: ConcurrentSkipListMap");
    }

    // 5. ConcurrentSkipListSet示例
    private static void skipListSetDemo() {
        ConcurrentSkipListSet<Integer> set = new ConcurrentSkipListSet<>();

        // 添加元素
        set.add(30);
        set.add(10);
        set.add(20);
        set.add(50);
        set.add(40);

        System.out.println("  Set内容（有序）: " + set);

        // 导航方法
        System.out.println("  floor(25): " + set.floor(25));
        System.out.println("  ceiling(25): " + set.ceiling(25));

        // 子集
        System.out.println("  subSet(15, 45): " + set.subSet(15, 45));

        // 可逆遍历
        System.out.println("  descendingSet:");
        NavigableSet<Integer> descending = set.descendingSet();
        descending.forEach(v -> System.out.println("    " + v));
    }
}
