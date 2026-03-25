package com.example.juc;

import java.util.concurrent.*;
import java.util.*;

/**
 * CopyOnWriteArraySet学习示例
 * 写时复制的线程安全Set
 */
public class CopyOnWriteArraySetExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== CopyOnWriteArraySet学习示例 ===\n");

        // 1. 基本使用
        System.out.println("1. 基本使用:");
        basicUsage();

        // 2. 并发读写场景
        System.out.println("\n2. 并发读写场景 - 读多写少:");
        concurrentReadWrite();

        // 3. 与HashSet对比
        System.out.println("\n3. CopyOnWriteArraySet vs HashSet:");
        comparison();

        // 4. 适用场景
        System.out.println("\n4. 适用场景分析:");
        useCases();
    }

    // 1. 基本使用
    private static void basicUsage() {
        CopyOnWriteArraySet<String> set = new CopyOnWriteArraySet<>();

        // 添加元素
        set.add("Apple");
        set.add("Banana");
        set.add("Cherry");
        set.add("Apple"); // 重复元素不会被添加

        System.out.println("  Set内容: " + set);
        System.out.println("  包含Banana: " + set.contains("Banana"));
        System.out.println("  包含Durian: " + set.contains("Durian"));
        System.out.println("  大小: " + set.size());

        // 删除元素
        set.remove("Banana");
        System.out.println("  删除Banana后: " + set);

        // 遍历
        System.out.println("  遍历Set:");
        for (String item : set) {
            System.out.println("    " + item);
        }
    }

    // 2. 并发读写场景
    private static void concurrentReadWrite() throws InterruptedException {
        CopyOnWriteArraySet<Integer> set = new CopyOnWriteArraySet<>();

        // 初始化一些数据
        for (int i = 1; i <= 10; i++) {
            set.add(i);
        }

        // 启动多个读线程
        Thread[] readers = new Thread[5];
        for (int i = 0; i < readers.length; i++) {
            final int readerId = i;
            readers[i] = new Thread(() -> {
                int sum = 0;
                for (int j = 0; j < 100; j++) {
                    // 读操作无需加锁，性能高
                    for (Integer num : set) {
                        sum += num;
                    }
                }
                System.out.println("  读者" + readerId + "计算总和: " + sum);
            }, "Reader-" + readerId);
        }

        // 启动写线程
        Thread writer = new Thread(() -> {
            Random random = new Random();
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(300);
                    // 写操作会复制整个数组
                    if (random.nextBoolean()) {
                        int newValue = 100 + i;
                        set.add(newValue);
                        System.out.println("  写者添加: " + newValue);
                    } else {
                        int index = random.nextInt(set.size());
                        Iterator<Integer> it = set.iterator();
                        for (int j = 0; j < index && it.hasNext(); j++) {
                            it.next();
                        }
                        if (it.hasNext()) {
                            int removed = it.next();
                            set.remove(removed);
                            System.out.println("  写者删除: " + removed);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Writer");

        // 启动所有线程
        for (Thread reader : readers) {
            reader.start();
        }
        writer.start();

        // 等待完成
        for (Thread reader : readers) {
            reader.join();
        }
        writer.join();

        System.out.println("  最终Set大小: " + set.size());
    }

    // 3. 与HashSet对比
    private static void comparison() {
        System.out.println("  CopyOnWriteArraySet:");
        System.out.println("    ✓ 线程安全");
        System.out.println("    ✓ 读操作无锁，性能高");
        System.out.println("    ✓ 写操作复制整个数组，性能低");
        System.out.println("    ✓ 迭代器弱一致性（不抛出ConcurrentModificationException）");
        System.out.println("    ✓ 内存占用较高");
        System.out.println("    ✓ 适合读多写少场景");
        System.out.println();
        System.out.println("  HashSet:");
        System.out.println("    ✓ 非线程安全");
        System.out.println("    ✓ 性能最高");
        System.out.println("    ✓ 内存占用低");
        System.out.println("    ✓ 需要手动同步");
        System.out.println();
        System.out.println("  Collections.synchronizedSet:");
        System.out.println("    ✓ 线程安全");
        System.out.println("    ✓ 所有操作都加锁");
        System.out.println("    ✓ 读写都阻塞");
        System.out.println("    ✓ 迭代时需要手动加锁");
    }

    // 4. 适用场景
    private static void useCases() {
        System.out.println("  适用场景:");
        System.out.println("    1. 事件监听器集合 - 读写频繁，修改很少");
        System.out.println("    2. 白名单/黑名单 - 初始化后很少修改");
        System.out.println("    3. 缓存键集合 - 读多写少");
        System.out.println("    4. 配置项集合 - 启动后基本不变");
        System.out.println();
        System.out.println("  不适用场景:");
        System.out.println("    1. 写操作频繁 - 每次写都复制数组");
        System.out.println("    2. 数据量大 - 复制开销大");
        System.out.println("    3. 需要强一致性 - 迭代器可能看到旧数据");
        System.out.println();
        System.out.println("  性能对比（假设1000次操作）:");
        System.out.println("    CopyOnWriteArraySet: 读1000次 ≈ 1ms");
        System.out.println("    CopyOnWriteArraySet: 写1000次 ≈ 1000ms");
        System.out.println("    HashSet: 写1000次 ≈ 1ms");
    }
}
