package com.example.juc;

import java.util.concurrent.*;
import java.util.Random;

/**
 * Phaser学习示例
 * 高级同步器 - 支持多阶段同步和动态注册/注销
 */
public class PhaserExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Phaser学习示例 ===\n");

        // 1. 基本使用
        System.out.println("1. 基本使用 - 多阶段同步:");
        basicPhaser();

        // 2. 动态注册/注销
        System.out.println("\n2. 动态注册/注销参与者:");
        dynamicRegistration();

        // 3. 与CyclicBarrier对比
        System.out.println("\n3. Phaser vs CyclicBarrier:");
        phaserVsBarrier();

        // 4. 实际场景 - 多阶段任务
        System.out.println("\n4. 实际场景 - 多阶段并行处理:");
        multiStageTask();
    }

    // 1. 基本使用
    private static void basicPhaser() throws InterruptedException {
        Phaser phaser = new Phaser(3); // 初始注册3个参与者

        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            new Thread(() -> {
                try {
                    // 阶段1
                    System.out.println("  任务" + taskId + " - 阶段1开始");
                    Thread.sleep(100 + taskId * 50);
                    System.out.println("  任务" + taskId + " - 阶段1完成，等待其他任务");
                    int phase = phaser.arriveAndAwaitAdvance();

                    // 阶段2
                    System.out.println("  任务" + taskId + " - 阶段2开始 (phase=" + phase + ")");
                    Thread.sleep(100 + taskId * 50);
                    System.out.println("  任务" + taskId + " - 阶段2完成，等待其他任务");
                    phase = phaser.arriveAndAwaitAdvance();

                    // 阶段3
                    System.out.println("  任务" + taskId + " - 阶段3开始 (phase=" + phase + ")");
                    Thread.sleep(100 + taskId * 50);
                    System.out.println("  任务" + taskId + " - 全部完成");
                    phaser.arriveAndDeregister(); // 完成并注销
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        Thread.sleep(2000);
    }

    // 2. 动态注册/注销
    private static void dynamicRegistration() throws InterruptedException {
        Phaser phaser = new Phaser();

        // 主线程注册
        phaser.register();
        System.out.println("  主线程注册，当前参与者: " + phaser.getRegisteredParties());

        // 动态创建工作线程
        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            new Thread(() -> {
                phaser.register();
                System.out.println("  任务" + taskId + " 注册，当前参与者: " + phaser.getRegisteredParties());

                try {
                    Thread.sleep(taskId * 200);
                    System.out.println("  任务" + taskId + " 完成");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // 任务完成时注销
                int arrived = phaser.arriveAndDeregister();
                System.out.println("  任务" + taskId + " 注销，剩余参与者: " + phaser.getRegisteredParties() + ", 到达数: " + arrived);
            }).start();
        }

        // 主线程等待
        phaser.arriveAndAwaitAdvance();
        System.out.println("  所有动态任务已完成");
    }

    // 3. Phaser vs CyclicBarrier
    private static void phaserVsBarrier() throws InterruptedException {
        System.out.println("  Phaser特性:");
        System.out.println("    ✓ 支持动态注册/注销参与者");
        System.out.println("    ✓ 支持多阶段同步");
        System.out.println("    ✓ 可以到达任意阶段");
        System.out.println("    ✓ 提供丰富的状态查询方法");
        System.out.println("    ✓ 可以设置到达时的回调");
        System.out.println();
        System.out.println("  CyclicBarrier特性:");
        System.out.println("    ✓ 参与者数量固定");
        System.out.println("    ✓ 可以循环使用");
        System.out.println("    ✓ 可以设置到达时的回调");
        System.out.println("    ✓ 适合固定数量线程的多阶段同步");
        System.out.println();
        System.out.println("  选择建议:");
        System.out.println("    - 固定参与者: CyclicBarrier更简单");
        System.out.println("    - 动态参与者: Phaser更灵活");
        System.out.println("    - 复杂多阶段: Phaser");
    }

    // 4. 实际场景 - 多阶段任务
    private static void multiStageTask() throws InterruptedException {
        // 创建Phaser，主线程作为第一个参与者
        Phaser phaser = new Phaser() {
            // 在阶段转换时执行回调
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                System.out.println("  === 阶段" + phase + "完成，所有参与者到达同步点 ===");
                // 返回true表示Phaser终止，false表示继续
                return phase >= 2 || registeredParties == 0;
            }
        };

        // 主线程注册并等待
        phaser.register();

        // 创建4个工作线程
        int workerCount = 4;
        for (int i = 1; i <= workerCount; i++) {
            final int workerId = i;
            new Thread(() -> {
                phaser.register();
                Random random = new Random();

                try {
                    // 阶段0: 初始化
                    System.out.println("  工人" + workerId + " - 初始化");
                    Thread.sleep(random.nextInt(200));
                    phaser.arriveAndAwaitAdvance();

                    // 阶段1: 数据加载
                    System.out.println("  工人" + workerId + " - 加载数据");
                    Thread.sleep(random.nextInt(300));
                    phaser.arriveAndAwaitAdvance();

                    // 阶段2: 处理数据
                    System.out.println("  工人" + workerId + " - 处理数据");
                    Thread.sleep(random.nextInt(400));

                    // 完成并注销
                    int phase = phaser.arriveAndDeregister();
                    System.out.println("  工人" + workerId + " - 完成，最终阶段: " + phase);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        // 主线程等待所有阶段完成
        int finalPhase = phaser.awaitAdvance(phaser.getPhase());
        System.out.println("  所有阶段完成，最终阶段: " + finalPhase);
    }
}
