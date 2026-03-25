package com.example.juc;

import java.util.concurrent.*;
import java.util.*;

/**
 * PriorityBlockingQueue学习示例
 * 优先级阻塞队列 - 按优先级顺序取出元素
 */
public class PriorityBlockingQueueExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== PriorityBlockingQueue学习示例 ===\n");

        // 1. 基本使用
        System.out.println("1. 基本使用 - 按优先级取出:");
        basicUsage();

        // 2. 自定义优先级
        System.out.println("\n2. 自定义优先级 - 任务调度:");
        customPriority();

        // 3. 实际场景 - 优先级任务队列
        System.out.println("\n3. 实际场景 - 多消费者处理优先级任务:");
        priorityTaskQueue();

        // 4. 与其他阻塞队列对比
        System.out.println("\n4. 与其他阻塞队列对比:");
        comparison();
    }

    // 1. 基本使用
    private static void basicUsage() {
        // Integer自然排序（小到大）
        PriorityBlockingQueue<Integer> queue = new PriorityBlockingQueue<>();

        // 随机添加元素
        queue.add(30);
        queue.add(10);
        queue.add(50);
        queue.add(20);
        queue.add(40);

        System.out.println("  添加顺序: 30, 10, 50, 20, 40");
        System.out.println("  取出顺序（按优先级）:");

        while (!queue.isEmpty()) {
            System.out.println("    " + queue.poll());
        }
    }

    // 2. 自定义优先级
    private static void customPriority() {
        // 按优先级从高到低
        PriorityBlockingQueue<Task> queue = new PriorityBlockingQueue<>();

        queue.add(new Task("低优先级任务", 1));
        queue.add(new Task("高优先级任务", 3));
        queue.add(new Task("中优先级任务", 2));
        queue.add(new Task("紧急任务", 5));
        queue.add(new Task("普通任务", 2));

        System.out.println("  任务按优先级执行:");

        while (!queue.isEmpty()) {
            Task task = queue.poll();
            System.out.println("    执行: " + task);
        }
    }

    // 3. 实际场景 - 多消费者处理优先级任务
    private static void priorityTaskQueue() throws InterruptedException {
        PriorityBlockingQueue<PriorityTask> taskQueue = new PriorityBlockingQueue<>();

        // 生产者 - 添加不同优先级的任务
        Thread producer = new Thread(() -> {
            Random random = new Random();
            String[] types = {"低", "中", "高", "紧急"};

            for (int i = 1; i <= 10; i++) {
                int priority = random.nextInt(4); // 0-3
                String type = types[priority];
                PriorityTask task = new PriorityTask("任务" + i, type, priority);

                taskQueue.put(task);
                System.out.println("  生产: " + task);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // 多个消费者
        int consumerCount = 2;
        Thread[] consumers = new Thread[consumerCount];

        for (int i = 0; i < consumerCount; i++) {
            final int consumerId = i + 1;
            consumers[i] = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        PriorityTask task = taskQueue.take();
                        System.out.println("  消费者" + consumerId + "处理: " + task);

                        // 模拟处理时间
                        Thread.sleep(200 + task.priority * 100);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Consumer-" + consumerId);
        }

        producer.start();
        for (Thread c : consumers) c.start();

        producer.join();
        Thread.sleep(500);

        // 中断消费者
        for (Thread c : consumers) c.interrupt();
        for (Thread c : consumers) c.join();
    }

    // 4. 与其他阻塞队列对比
    private static void comparison() {
        System.out.println("  PriorityBlockingQueue:");
        System.out.println("    ✓ 基于堆实现");
        System.out.println("    ✓ 按优先级顺序取出元素");
        System.out.println("    ✓ 无界队列（可无限增长）");
        System.out.println("    ✓ 需要元素实现Comparable或提供Comparator");
        System.out.println();
        System.out.println("  ArrayBlockingQueue:");
        System.out.println("    ✓ 有界，基于数组");
        System.out.println("    ✓ FIFO顺序");
        System.out.println("    ✓ 可以配置公平性");
        System.out.println();
        System.out.println("  LinkedBlockingQueue:");
        System.out.println("    ✓ 可选有界/无界");
        System.out.println("    ✓ FIFO顺序");
        System.out.println("    ✓ 基于链表");
        System.out.println();
        System.out.println("  选择建议:");
        System.out.println("    - 需要优先级处理: PriorityBlockingQueue");
        System.out.println("    - 生产者-消费者: LinkedBlockingQueue");
        System.out.println("    - 固定容量: ArrayBlockingQueue");
    }

    // 任务类
    static class Task implements Comparable<Task> {
        private final String name;
        private final int priority; // 1=低, 3=高

        public Task(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        @Override
        public int compareTo(Task other) {
            // 优先级高的先执行（降序）
            return Integer.compare(other.priority, this.priority);
        }

        @Override
        public String toString() {
            return name + "(优先级:" + priority + ")";
        }
    }

    // 优先级任务
    static class PriorityTask implements Comparable<PriorityTask> {
        final String name;
        final String type;
        final int priority; // 0=低, 1=中, 2=高, 3=紧急

        public PriorityTask(String name, String type, int priority) {
            this.name = name;
            this.type = type;
            this.priority = priority;
        }

        @Override
        public int compareTo(PriorityTask other) {
            return Integer.compare(other.priority, this.priority);
        }

        @Override
        public String toString() {
            return name + "[" + type + "]";
        }
    }
}
