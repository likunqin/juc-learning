package com.example.juc;

import java.io.*;
import java.util.concurrent.*;

/**
 * 线程间通信示例
 * <p>
 * 演示 Java 中多种线程间通信的方式
 * </p>
 *
 * <h3>主要通信方式：</h3>
 * <ul>
 *   <li>wait/notify/notifyAll - Object 类的方法</li>
 *   <li>Condition - Lock 接口的条件变量</li>
 *   <li>PipedInputStream/PipedOutputStream - 管道通信</li>
 *   <li>join() - 等待线程结束</li>
 *   <li>共享变量 - 通过 volatile 或锁保护</li>
 * </ul>
 */
public class ThreadCommunicationExample {

    /**
     * 场景1：wait/notify/notifyAll 基础使用
     * <p>
     * 演示最传统的线程间通信方式
     * </p>
     */
    public static void waitNotifyBasic() throws InterruptedException {
        System.out.println("=== 场景1：wait/notify 基础 ===");

        class SharedResource {
            private boolean flag = false;

            public synchronized void waitForFlag() throws InterruptedException {
                System.out.println("等待线程开始等待...");
                while (!flag) {
                    wait(); // 释放锁并等待
                }
                System.out.println("等待线程被唤醒，flag = " + flag);
            }

            public synchronized void setFlag(boolean value) {
                System.out.println("设置线程设置 flag = " + value);
                this.flag = value;
                notify(); // 唤醒一个等待线程
            }
        }

        SharedResource resource = new SharedResource();

        Thread waiter = new Thread(() -> {
            try {
                resource.waitForFlag();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread setter = new Thread(() -> {
            try {
                Thread.sleep(1000); // 确保等待线程先启动
                resource.setFlag(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        waiter.start();
        setter.start();

        waiter.join();
        setter.join();
    }

    /**
     * 场景2：生产者-消费者（wait/notify）
     * <p>
     * 使用 wait/notify 实现经典的生产者消费者模式
     * </p>
     */
    public static void producerConsumerWaitNotify() throws InterruptedException {
        System.out.println("\n=== 场景2：生产者-消费者（wait/notify） ===");

        class Buffer {
            private int data;
            private boolean available = false;

            public synchronized int consume() throws InterruptedException {
                while (!available) {
                    System.out.println("消费者等待数据...");
                    wait();
                }
                available = false;
                System.out.println("消费: " + data);
                notifyAll(); // 通知生产者
                return data;
            }

            public synchronized void produce(int value) throws InterruptedException {
                while (available) {
                    System.out.println("生产者等待缓冲区空...");
                    wait();
                }
                data = value;
                available = true;
                System.out.println("生产: " + data);
                notifyAll(); // 通知消费者
            }
        }

        Buffer buffer = new Buffer();

        // 生产者
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    buffer.produce(i);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 消费者
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    buffer.consume();
                    Thread.sleep(700);
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

    /**
     * 场景3：notify vs notifyAll
     * <p>
     * 演示 notify 和 notifyAll 的区别
     * </p>
     */
    public static void notifyVsNotifyAll() throws InterruptedException {
        System.out.println("\n=== 场景3：notify vs notifyAll ===");

        class Resource {
            private int value = 0;
            private boolean ready = false;

            public synchronized int getValue() throws InterruptedException {
                while (!ready) {
                    System.out.println(Thread.currentThread().getName() + " 等待...");
                    wait();
                }
                System.out.println(Thread.currentThread().getName() + " 获取值: " + value);
                ready = false;
                notifyAll();
                return value;
            }

            public synchronized void setValue(int value) {
                this.value = value;
                this.ready = true;
                System.out.println("设置值: " + value);
                notifyAll(); // 唤醒所有等待线程
            }
        }

        Resource resource = new Resource();

        // 创建多个等待线程
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    resource.getValue();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "等待线程" + i).start();
        }

        Thread.sleep(500);

        // 设置值
        Thread setter = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                resource.setValue(i * 100);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        setter.start();
        setter.join();
    }

    /**
     * 场景4：使用 join() 等待线程结束
     * <p>
     * 演示 join() 方法的基本用法和超时版本
     * </p>
     */
    public static void joinExample() throws InterruptedException {
        System.out.println("\n=== 场景4：join() 等待线程 ===");

        class Worker implements Runnable {
            private final String name;
            private final long workTime;

            public Worker(String name, long workTime) {
                this.name = name;
                this.workTime = workTime;
            }

            @Override
            public void run() {
                try {
                    System.out.println(name + " 开始工作，需要 " + workTime + "ms");
                    Thread.sleep(workTime);
                    System.out.println(name + " 工作完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // 创建多个工作线程
        Thread[] workers = {
                new Thread(new Worker("Worker-1", 1000)),
                new Thread(new Worker("Worker-2", 1500)),
                new Thread(new Worker("Worker-3", 800))
        };

        System.out.println("主线程启动所有工作线程");
        for (Thread worker : workers) {
            worker.start();
        }

        // 等待所有工作线程完成
        for (Thread worker : workers) {
            worker.join();
        }

        System.out.println("所有工作线程完成，主线程继续");

        // 演示带超时的 join
        System.out.println("\n演示带超时的 join:");
        Thread longTask = new Thread(new Worker("LongTask", 3000));
        longTask.start();

        long start = System.currentTimeMillis();
        longTask.join(1000); // 等待最多1秒
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("等待时间: " + elapsed + "ms，线程状态: " +
                (longTask.isAlive() ? "仍在运行" : "已完成"));
    }

    /**
     * 场景5：管道通信 - PipedInputStream/PipedOutputStream
     * <p>
     * 演示使用管道进行线程间数据传输
     * </p>
     */
    public static void pipeCommunication() throws InterruptedException, IOException {
        System.out.println("\n=== 场景5：管道通信 ===");

        PipedInputStream input = new PipedInputStream();
        PipedOutputStream output = new PipedOutputStream(input);

        // 发送线程
        Thread sender = new Thread(() -> {
            try {
                String[] messages = {"消息1", "消息2", "消息3", "消息4"};
                for (String msg : messages) {
                    output.write(msg.getBytes());
                    output.flush();
                    System.out.println("发送: " + msg);
                    Thread.sleep(500);
                }
                output.close();
                System.out.println("发送完成");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // 接收线程
        Thread receiver = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    String msg = new String(buffer, 0, bytesRead);
                    System.out.println("接收: " + msg);
                }
                System.out.println("接收完成");
            } catch (IOException e) {
                // 正常关闭时会抛出异常
            }
        });

        sender.start();
        receiver.start();

        sender.join();
        receiver.join();
    }

    /**
     * 场景6：通过共享变量通信
     * <p>
     * 演示使用 volatile 变量和原子类进行简单通信
     * </p>
     */
    public static void sharedVariableCommunication() throws InterruptedException {
        System.out.println("\n=== 场景6：共享变量通信 ===");

        class SharedState {
            private volatile boolean stopRequested = false;
            private AtomicInteger counter = new AtomicInteger(0);

            public void requestStop() {
                stopRequested = true;
                System.out.println("请求停止信号");
            }

            public boolean isStopRequested() {
                return stopRequested;
            }

            public int getCounter() {
                return counter.get();
            }

            public void incrementCounter() {
                counter.incrementAndGet();
            }
        }

        SharedState state = new SharedState();

        // 工作线程
        Thread worker = new Thread(() -> {
            int count = 0;
            while (!state.isStopRequested()) {
                state.incrementCounter();
                count++;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("工作线程停止，执行了 " + count + " 次操作");
        });

        worker.start();

        // 主线程等待3秒后请求停止
        Thread.sleep(3000);
        state.requestStop();

        worker.join();
        System.out.println("最终计数: " + state.getCounter());
    }

    /**
     * 场景7：多线程协作完成任务
     * <p>
     * 多个线程协同完成一个任务
     * </p>
     */
    public static void multiThreadCooperation() throws InterruptedException {
        System.out.println("\n=== 场景7：多线程协作 ===");

        class TaskCoordinator {
            private String stage1Result;
            private String stage2Result;
            private boolean stage1Done = false;
            private boolean stage2Done = false;

            public synchronized void stage1Complete(String result) {
                this.stage1Result = result;
                this.stage1Done = true;
                System.out.println("阶段1完成: " + result);
                notifyAll();
            }

            public synchronized void stage2Complete(String result) {
                this.stage2Result = result;
                this.stage2Done = true;
                System.out.println("阶段2完成: " + result);
                notifyAll();
            }

            public synchronized void waitForStage1() throws InterruptedException {
                while (!stage1Done) {
                    wait();
                }
            }

            public synchronized void waitForStage2() throws InterruptedException {
                while (!stage2Done) {
                    wait();
                }
            }

            public synchronized String getFinalResult() {
                return stage1Result + " + " + stage2Result + " = 最终结果";
            }
        }

        TaskCoordinator coordinator = new TaskCoordinator();

        // 阶段1线程
        Thread stage1Thread = new Thread(() -> {
            try {
                Thread.sleep(1000);
                coordinator.stage1Complete("数据A");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 阶段2线程
        Thread stage2Thread = new Thread(() -> {
            try {
                Thread.sleep(1500);
                coordinator.stage2Complete("数据B");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 汇总线程
        Thread summaryThread = new Thread(() -> {
            try {
                coordinator.waitForStage1();
                coordinator.waitForStage2();
                System.out.println(coordinator.getFinalResult());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        stage1Thread.start();
        stage2Thread.start();
        summaryThread.start();

        stage1Thread.join();
        stage2Thread.join();
        summaryThread.join();
    }

    /**
     * 最佳实践与注意事项
     */
    public static void bestPractices() {
        System.out.println("\n=== 最佳实践与注意事项 ===");

        System.out.println("""
        1. wait/notify 必须在同步块中调用
           - 必须持有对象的监视器锁
           - 否则抛出 IllegalMonitorStateException

        2. 使用 while 循环检查条件
           - 避免"虚假唤醒"问题
           - 唤醒后需要重新检查条件

        3. 优先使用 notifyAll
           - notify 只唤醒一个线程
           - notifyAll 唤醒所有等待线程更安全
           - 除非确定只有一个等待线程

        4. 管道通信注意事项
           - PipedInputStream/PipedOutputStream 用法较复杂
           - 缓冲区满了会阻塞
           - 推荐使用 BlockingQueue 替代

        5. join() 的使用
           - 等待线程完成
           - 注意设置超时避免永久等待
           - join() 不响应中断

        6. 共享变量通信
           - 使用 volatile 保证可见性
           - 复杂操作使用原子类或锁
           - 避免竞态条件

        7. Condition vs wait/notify
           - Condition 功能更强大，推荐使用
           - 支持多条件队列
           - API 更清晰明确

        8. 选择合适的通信方式
           - 简单场景：volatile + 标志位
           - 生产者消费者：BlockingQueue 或 Condition
           - 复杂协调：CountDownLatch/CyclicBarrier
        """);
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        waitNotifyBasic();
        producerConsumerWaitNotify();
        notifyVsNotifyAll();
        joinExample();
        pipeCommunication();
        sharedVariableCommunication();
        multiThreadCooperation();
        bestPractices();
    }
}
