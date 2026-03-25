package com.example.juc;

import java.util.concurrent.*;
import java.util.Random;

/**
 * Exchanger学习示例
 * 线程间数据交换 - 两个线程在同步点交换数据
 */
public class ExchangerExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Exchanger学习示例 ===\n");

        // 1. 基本使用
        System.out.println("1. 基本使用 - 两个线程交换数据:");
        basicExchange();

        // 2. 实际场景 - 数据缓冲
        System.out.println("\n2. 实际场景 - 生产者消费者缓冲区交换:");
        bufferExchange();

        // 3. 超时交换
        System.out.println("\n3. 带超时的交换:");
        exchangeWithTimeout();

        // 4. 与其他同步器对比
        System.out.println("\n4. Exchanger vs 其他同步器:");
        comparison();
    }

    // 1. 基本使用
    private static void basicExchange() throws InterruptedException {
        Exchanger<String> exchanger = new Exchanger<>();

        // 线程A
        Thread threadA = new Thread(() -> {
            try {
                String dataFromA = "来自A的数据";
                System.out.println("  线程A准备发送: " + dataFromA);

                // exchange会阻塞，直到另一个线程调用exchange
                String dataFromB = exchanger.exchange(dataFromA);

                System.out.println("  线程A收到: " + dataFromB);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-A");

        // 线程B
        Thread threadB = new Thread(() -> {
            try {
                String dataFromB = "来自B的数据";
                System.out.println("  线程B准备发送: " + dataFromB);

                String dataFromA = exchanger.exchange(dataFromB);

                System.out.println("  线程B收到: " + dataFromA);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-B");

        threadA.start();
        threadB.start();
        threadA.join();
        threadB.join();
    }

    // 2. 实际场景 - 数据缓冲区交换
    private static void bufferExchange() throws InterruptedException {
        // 两个缓冲区，生产者填充一个，消费者处理一个
        // 当缓冲区满时，交换缓冲区
        Exchanger<Buffer> exchanger = new Exchanger<>();

        // 生产者 - 填充缓冲区A
        Thread producer = new Thread(() -> {
            Buffer buffer = new Buffer("缓冲区A");
            Random random = new Random();

            try {
                for (int i = 0; i < 5; i++) {
                    // 填充数据
                    while (buffer.size() < buffer.capacity()) {
                        int data = random.nextInt(100);
                        buffer.add(data);
                        System.out.println("  生产者添加: " + data);
                        Thread.sleep(50);
                    }

                    System.out.println("  生产者缓冲区已满: " + buffer);

                    // 交换缓冲区
                    buffer = exchanger.exchange(buffer);
                    System.out.println("  生产者获得空缓冲区: " + buffer.name);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 消费者 - 处理缓冲区B
        Thread consumer = new Thread(() -> {
            Buffer buffer = new Buffer("缓冲区B");

            try {
                for (int i = 0; i < 5; i++) {
                    // 等待获取填充的缓冲区
                    buffer = exchanger.exchange(buffer);
                    System.out.println("  消费者获得缓冲区: " + buffer);

                    // 处理数据
                    while (!buffer.isEmpty()) {
                        int data = buffer.remove();
                        System.out.println("  消费者处理: " + data);
                        Thread.sleep(80);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }

    // 3. 带超时的交换
    private static void exchangeWithTimeout() throws InterruptedException {
        Exchanger<String> exchanger = new Exchanger<>();

        // 只启动一个线程
        Thread singleThread = new Thread(() -> {
            try {
                System.out.println("  线程准备交换...");
                // 等待1秒，如果没有交换伙伴则返回
                String result = exchanger.exchange("我的数据", 1, TimeUnit.SECONDS);
                System.out.println("  交换成功: " + result);
            } catch (TimeoutException e) {
                System.out.println("  交换超时: 没有交换伙伴");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        singleThread.start();
        singleThread.join();
    }

    // 4. 与其他同步器对比
    private static void comparison() {
        System.out.println("  Exchanger:");
        System.out.println("    ✓ 用于两个线程之间交换数据");
        System.out.println("    ✓ 交换点同步，双方都到达才继续");
        System.out.println("    ✓ 适用于数据缓冲区交换、管道通信");
        System.out.println();
        System.out.println("  CyclicBarrier:");
        System.out.println("    ✓ 多个线程等待到同步点");
        System.out.println("    ✓ 可以到达时执行回调");
        System.out.println("    ✓ 可以循环使用");
        System.out.println();
        System.out.println("  CountDownLatch:");
        System.out.println("    ✓ 一个或多个线程等待其他线程完成");
        System.out.println("    ✓ 计数器减到0后无法重用");
        System.out.println();
        System.out.println("  Phaser:");
        System.out.println("    ✓ 支持动态注册/注销线程");
        System.out.println("    ✓ 支持多阶段同步");
        System.out.println("    ✓ 更灵活的同步器");
    }

    // 简单缓冲区
    static class Buffer {
        private final String name;
        private final java.util.LinkedList<Integer> data = new java.util.LinkedList<>();
        private final int capacity = 5;

        public Buffer(String name) {
            this.name = name;
        }

        public void add(int item) {
            if (data.size() < capacity) {
                data.add(item);
            }
        }

        public int remove() {
            return data.isEmpty() ? -1 : data.remove();
        }

        public int size() {
            return data.size();
        }

        public int capacity() {
            return capacity;
        }

        public boolean isEmpty() {
            return data.isEmpty();
        }

        @Override
        public String toString() {
            return name + "(" + size() + "/" + capacity() + ")";
        }
    }
}
