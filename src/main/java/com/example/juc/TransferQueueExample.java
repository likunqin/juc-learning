package com.example.juc;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TransferQueue学习示例
 * 生产者可以等待消费者接收的阻塞队列
 */
public class TransferQueueExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== TransferQueue学习示例 ===\n");

        // 1. 基本使用 - transfer
        System.out.println("1. 基本使用 - transfer()等待消费者:");
        basicTransfer();

        // 2. tryTransfer非阻塞模式
        System.out.println("\n2. tryTransfer()非阻塞模式:");
        tryTransferDemo();

        // 3. tryTransfer带超时
        System.out.println("\n3. tryTransfer()带超时:");
        tryTransferTimeout();

        // 4. 实际场景 - 手递手模式
        System.out.println("\n4. 实际场景 - 手递手数据传输:");
        handOffScenario();

        // 5. 与BlockingQueue对比
        System.out.println("\n5. TransferQueue vs BlockingQueue:");
        comparison();
    }

    // 1. 基本使用
    private static void basicTransfer() throws InterruptedException {
        TransferQueue<String> queue = new LinkedTransferQueue<>();

        // 消费者线程
        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(1000); // 延迟1秒再消费
                String item = queue.take();
                System.out.println("  消费者接收: " + item);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 生产者线程
        Thread producer = new Thread(() -> {
            try {
                System.out.println("  生产者准备传输数据...");
                queue.transfer("重要数据"); // 阻塞直到有消费者接收
                System.out.println("  生产者数据已被接收");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }

    // 2. tryTransfer非阻塞模式
    private static void tryTransferDemo() throws InterruptedException {
        TransferQueue<String> queue = new LinkedTransferQueue<>();

        // 非阻塞传输 - 如果没有等待的消费者则立即返回false
        boolean transferred = queue.tryTransfer("数据1");
        System.out.println("  tryTransfer结果（无等待消费者）: " + transferred);

        // 预先启动一个消费者
        Thread consumer = new Thread(() -> {
            try {
                String item = queue.take();
                System.out.println("  消费者接收: " + item);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 等待消费者进入等待状态
        consumer.start();
        Thread.sleep(100);

        // 现在有等待的消费者，transfer应该成功
        transferred = queue.tryTransfer("数据2");
        System.out.println("  tryTransfer结果（有等待消费者）: " + transferred);

        consumer.join();
    }

    // 3. tryTransfer带超时
    private static void tryTransferTimeout() throws InterruptedException {
        TransferQueue<String> queue = new LinkedTransferQueue<>();

        Thread producer = new Thread(() -> {
            try {
                System.out.println("  生产者尝试传输（超时1秒）...");
                boolean transferred = queue.tryTransfer("超时数据", 1, TimeUnit.SECONDS);
                System.out.println("  传输结果: " + transferred);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        producer.join();

        // 添加消费者，看看是否能接收到残留数据
        Thread consumer = new Thread(() -> {
            try {
                String item = queue.poll(500, TimeUnit.MILLISECONDS);
                System.out.println("  消费者尝试取出: " + item);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumer.start();
        consumer.join();
    }

    // 4. 手递手场景 - 实时数据传输
    private static void handOffScenario() throws InterruptedException {
        TransferQueue<DataPacket> queue = new LinkedTransferQueue<>();
        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);

        // 生产者
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                DataPacket packet = new DataPacket("Packet-" + i, i * 100);
                produced.incrementAndGet();

                System.out.println("  生产者准备传输: " + packet);

                try {
                    // transfer会等待消费者，确保实时传输
                    queue.transfer(packet);
                    System.out.println("  生产者 " + packet.id + " 已传输");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // 消费者
        Thread consumer = new Thread(() -> {
            while (consumed.get() < 5) {
                try {
                    DataPacket packet = queue.take();
                    consumed.incrementAndGet();

                    System.out.println("  消费者接收: " + packet);
                    System.out.println("  消费者处理 " + packet.id + "...");

                    // 模拟处理时间
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();

        System.out.println("  生产: " + produced.get() + ", 消费: " + consumed.get());
    }

    // 5. 与BlockingQueue对比
    private static void comparison() {
        System.out.println("  TransferQueue特性:");
        System.out.println("    ✓ transfer() - 等待消费者接收");
        System.out.println("    ✓ tryTransfer() - 立即返回，不等待");
        System.out.println("    ✓ tryTransfer(timeout) - 带超时等待");
        System.out.println("    ✓ 生产者知道数据是否被接收");
        System.out.println("    ✓ 适用于实时/手递手场景");
        System.out.println();
        System.out.println("  BlockingQueue特性:");
        System.out.println("    ✓ put() - 阻塞直到队列有空间");
        System.out.println("    ✓ offer() - 非阻塞，立即返回");
        System.out.println("    ✓ 生产者不关心谁消费");
        System.out.println("    ✓ 数据存储在队列中");
        System.out.println();
        System.out.println("  使用场景对比:");
        System.out.println("    - BlockingQueue: 生产者-消费者，有缓冲");
        System.out.println("    - TransferQueue: 实时传输，无缓冲，同步交付");
    }

    // 数据包
    static class DataPacket {
        final String id;
        final int payload;

        public DataPacket(String id, int payload) {
            this.id = id;
            this.payload = payload;
        }

        @Override
        public String toString() {
            return id + "(payload=" + payload + ")";
        }
    }
}
