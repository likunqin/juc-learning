package com.example.juc;

import java.util.concurrent.*;
import java.util.Random;

/**
 * DelayQueue学习示例
 * 延迟队列 - 元素只有在到期时才能被取出
 */
public class DelayQueueExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== DelayQueue学习示例 ===\n");

        // 1. 基本使用
        System.out.println("1. 基本使用:");
        basicUsage();

        // 2. 实际场景 - 任务调度
        System.out.println("\n2. 实际场景 - 延迟任务调度:");
        taskScheduling();

        // 3. 实际场景 - 缓存过期
        System.out.println("\n3. 实际场景 - 缓存自动过期:");
        cacheExpiration();

        // 4. 动态添加任务
        System.out.println("\n4. 动态添加延迟任务:");
        dynamicTasks();
    }

    // 1. 基本使用
    private static void basicUsage() throws InterruptedException {
        DelayQueue<DelayedElement> queue = new DelayQueue<>();

        // 添加不同延迟的元素
        queue.put(new DelayedElement("任务1", 1000));
        queue.put(new DelayedElement("任务2", 3000));
        queue.put(new DelayedElement("任务3", 2000));

        System.out.println("  添加了3个任务，按到期时间排序");

        // 按到期顺序取出
        while (!queue.isEmpty()) {
            DelayedElement element = queue.take(); // 阻塞直到有元素到期
            System.out.println("  取出: " + element);
        }
    }

    // 2. 任务调度
    private static void taskScheduling() throws InterruptedException {
        DelayQueue<ScheduledTask> scheduler = new DelayQueue<>();

        // 添加定时任务
        long now = System.currentTimeMillis();
        scheduler.put(new ScheduledTask("任务A", now + 1000));
        scheduler.put(new ScheduledTask("任务B", now + 3000));
        scheduler.put(new ScheduledTask("任务C", now + 2000));

        // 启动消费者线程
        Thread consumer = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    ScheduledTask task = scheduler.take();
                    task.execute();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        consumer.start();

        consumer.join();
    }

    // 3. 缓存过期
    private static void cacheExpiration() throws InterruptedException {
        DelayQueue<CacheEntry<String>> cache = new DelayQueue<>();

        // 添加缓存条目（5秒后过期）
        cache.put(new CacheEntry<>("user:1", "张三", 5000));
        cache.put(new CacheEntry<>("user:2", "李四", 3000));

        System.out.println("  缓存已创建，3-5秒后自动清理");

        // 缓存清理线程
        Thread cleaner = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    CacheEntry<String> expired = cache.take();
                    System.out.println("  缓存过期: " + expired.key);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        cleaner.start();

        cleaner.join();
    }

    // 4. 动态添加任务
    private static void dynamicTasks() throws InterruptedException {
        DelayQueue<DelayedElement> queue = new DelayQueue<>();

        // 添加任务的生产者
        Thread producer = new Thread(() -> {
            Random random = new Random();
            for (int i = 1; i <= 5; i++) {
                long delay = random.nextInt(3000) + 1000;
                queue.put(new DelayedElement("动态任务" + i, delay));
                System.out.println("  添加动态任务" + i + "，延迟" + delay + "ms");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // 消费者
        Thread consumer = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    DelayedElement element = queue.take();
                    System.out.println("  执行: " + element);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();

        producer.join();
        consumer.interrupt();
        consumer.join();
    }

    // 延迟元素实现
    static class DelayedElement implements java.util.concurrent.Delayed {
        private final String name;
        private final long endTime;

        public DelayedElement(String name, long delayMillis) {
            this.name = name;
            this.endTime = System.currentTimeMillis() + delayMillis;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long remaining = endTime - System.currentTimeMillis();
            return unit.convert(remaining, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(java.util.concurrent.Delayed other) {
            if (this == other) return 0;
            DelayedElement that = (DelayedElement) other;
            return Long.compare(this.endTime, that.endTime);
        }

        @Override
        public String toString() {
            long remaining = Math.max(0, endTime - System.currentTimeMillis());
            return name + " (剩余: " + remaining + "ms)";
        }
    }

    // 定时任务
    static class ScheduledTask implements java.util.concurrent.Delayed {
        private final String name;
        private final long executeTime;

        public ScheduledTask(String name, long executeTime) {
            this.name = name;
            this.executeTime = executeTime;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(executeTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(java.util.concurrent.Delayed other) {
            ScheduledTask that = (ScheduledTask) other;
            return Long.compare(this.executeTime, that.executeTime);
        }

        public void execute() {
            System.out.println("  执行任务: " + name + " at " + System.currentTimeMillis());
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // 缓存条目
    static class CacheEntry<V> implements java.util.concurrent.Delayed {
        final String key;
        final V value;
        final long expireTime;

        public CacheEntry(String key, V value, long ttlMillis) {
            this.key = key;
            this.value = value;
            this.expireTime = System.currentTimeMillis() + ttlMillis;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(expireTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(java.util.concurrent.Delayed other) {
            CacheEntry<?> that = (CacheEntry<?>) other;
            return Long.compare(this.expireTime, that.expireTime);
        }
    }
}
