package com.example.juc;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CountDownLatch;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * VarHandle 学习示例
 * JDK 9+ 引入的原子操作新方式
 *
 * VarHandle 是对变量、数组元素、字段等的引用，提供了一套标准化的
 * 原子访问和内存屏障操作，可以替代 sun.misc.Unsafe
 *
 * 运行要求: JDK 9+
 */
public class VarHandleExample {

    // 测试用的字段
    private volatile int value = 0;
    private static int staticValue = 0;
    private final int[] array = new int[10];

    // 用于对比的 AtomicInteger
    private final AtomicInteger atomicInt = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        System.out.println("=== VarHandle 学习示例 (JDK 9+) ===\n");

        // 1. VarHandle 基础使用
        System.out.println("1. VarHandle 基础使用:");
        basicUsage();

        // 2. 原子操作
        System.out.println("\n2. VarHandle 原子操作:");
        atomicOperations();

        // 3. CAS 操作
        System.out.println("\n3. VarHandle CAS 操作:");
        casOperations();

        // 4. 数组元素访问
        System.out.println("\n4. VarHandle 数组元素访问:");
        arrayAccess();

        // 5. 静态字段访问
        System.out.println("\n5. VarHandle 静态字段访问:");
        staticFieldAccess();

        // 6. 内存屏障
        System.out.println("\n6. VarHandle 内存屏障:");
        memoryFences();

        // 7. VarHandle vs AtomicInteger 性能对比
        System.out.println("\n7. VarHandle vs AtomicInteger 性能对比:");
        performanceComparison();

        // 8. VarHandle 模拟 AtomicReference
        System.out.println("\n8. VarHandle 模拟 AtomicReference:");
        simulateAtomicReference();
    }

    // 1. VarHandle 基础使用
    private static void basicUsage() throws Exception {
        VarHandleExample example = new VarHandleExample();

        // 获取实例字段的 VarHandle
        VarHandle valueHandle = MethodHandles
            .privateLookupIn(VarHandleExample.class, MethodHandles.lookup())
            .findVarHandle(VarHandleExample.class, "value", int.class);

        // 读取值
        int currentValue = (int) valueHandle.get(example);
        System.out.println("  当前值: " + currentValue);

        // 设置值
        valueHandle.set(example, 42);
        System.out.println("  设置后值: " + valueHandle.get(example));

        // 比较并设置
        boolean success = valueHandle.compareAndSet(example, 42, 100);
        System.out.println("  CAS (42->100) " + (success ? "成功" : "失败") + ", 当前值: " + valueHandle.get(example));

        // 获取并添加
        int oldValue = (int) valueHandle.getAndAdd(example, 10);
        System.out.println("  getAndAdd(10) 旧值: " + oldValue + ", 新值: " + valueHandle.get(example));
    }

    // 2. VarHandle 原子操作
    private static void atomicOperations() throws Exception {
        VarHandleExample example = new VarHandleExample();

        VarHandle valueHandle = MethodHandles
            .privateLookupIn(VarHandleExample.class, MethodHandles.lookup())
            .findVarHandle(VarHandleExample.class, "value", int.class);

        System.out.println("  支持的原子操作方法:");
        System.out.println("    - get/set: 获取/设置值");
        System.out.println("    - getVolatile/setVolatile: 带内存屏障的读写");
        System.out.println("    - getOpaque/setOpaque: 带排他语义的读写");
        System.out.println("    - getAcquire/setRelease: 获取/释放语义的读写");
        System.out.println("    - compareAndSet: CAS 操作");
        System.out.println("    - compareAndExchange: CAS 并返回旧值");
        System.out.println("    - getAndAdd/getAndSet: 获取并操作");
        System.out.println("    - addAndGet/setAndGet: 操作并获取");
    }

    // 3. CAS 操作
    private static void casOperations() throws Exception {
        VarHandleExample example = new VarHandleExample();

        VarHandle valueHandle = MethodHandles
            .privateLookupIn(VarHandleExample.class, MethodHandles.lookup())
            .findVarHandle(VarHandleExample.class, "value", int.class);

        valueHandle.set(example, 0);

        System.out.println("  compareAndSet 示例:");
        // 期望值是 0，更新为 10
        boolean result1 = valueHandle.compareAndSet(example, 0, 10);
        System.out.println("    CAS(0->10): " + result1 + ", 当前值: " + valueHandle.get(example));

        // 期望值是 0，更新为 20（会失败）
        boolean result2 = valueHandle.compareAndSet(example, 0, 20);
        System.out.println("    CAS(0->20): " + result2 + ", 当前值: " + valueHandle.get(example));

        System.out.println("\n  compareAndExchange 示例:");
        // CAS 并返回旧值
        int old1 = (int) valueHandle.compareAndExchange(example, 10, 20);
        System.out.println("    CAE(10->20): 旧值=" + old1 + ", 当前值=" + valueHandle.get(example));

        int old2 = (int) valueHandle.compareAndExchange(example, 10, 30);
        System.out.println("    CAE(10->30): 旧值=" + old2 + ", 当前值=" + valueHandle.get(example) + " (期望值不匹配)");
    }

    // 4. 数组元素访问
    private static void arrayAccess() throws Exception {
        VarHandleExample example = new VarHandleExample();

        // 获取数组元素的 VarHandle
        VarHandle arrayHandle = MethodHandles
            .arrayElementVarHandle(int[].class);

        // 设置数组元素
        arrayHandle.set(example.array, 0, 100);
        arrayHandle.set(example.array, 1, 200);
        arrayHandle.set(example.array, 2, 300);

        System.out.println("  数组[0]: " + arrayHandle.get(example.array, 0));
        System.out.println("  数组[1]: " + arrayHandle.get(example.array, 1));
        System.out.println("  数组[2]: " + arrayHandle.get(example.array, 2));

        // 原子更新数组元素
        int old = (int) arrayHandle.getAndAdd(example.array, 1, 50);
        System.out.println("  数组[1] getAndAdd(50): 旧值=" + old + ", 新值=" + arrayHandle.get(example.array, 1));
    }

    // 5. 静态字段访问
    private static void staticFieldAccess() throws Exception {
        // 获取静态字段的 VarHandle
        VarHandle staticHandle = MethodHandles
            .privateLookupIn(VarHandleExample.class, MethodHandles.lookup())
            .findStaticVarHandle(VarHandleExample.class, "staticValue", int.class);

        staticHandle.set(100);
        System.out.println("  静态字段值: " + staticHandle.get());

        // 原子操作
        int old = (int) staticHandle.getAndAdd(50);
        System.out.println("  getAndAdd(50): 旧值=" + old + ", 新值=" + staticHandle.get());
    }

    // 6. 内存屏障
    private static void memoryFences() throws Exception {
        VarHandleExample example = new VarHandleExample();

        VarHandle valueHandle = MethodHandles
            .privateLookupIn(VarHandleExample.class, MethodHandles.lookup())
            .findVarHandle(VarHandleExample.class, "value", int.class);

        System.out.println("  内存屏障方法:");
        System.out.println("    - fullFence(): 完全内存屏障");
        System.out.println("    - acquireFence(): 获取语义屏障");
        System.out.println("    - releaseFence(): 释放语义屏障");
        System.out.println("    - loadLoadFence(): 读读屏障");
        System.out.println("    - storeStoreFence(): 写写屏障");

        // 完全内存屏障示例
        valueHandle.set(example, 1);
        VarHandle.fullFence();  // 确保前面的写操作对其他线程可见
        int value = (int) valueHandle.get(example);
        System.out.println("  使用 fullFence 后的值: " + value);
    }

    // 7. VarHandle vs AtomicInteger 性能对比
    private static void performanceComparison() throws Exception {
        final int THREADS = 10;
        final int ITERATIONS = 100_000;
        final int WARMUP = 10_000;

        VarHandleExample example = new VarHandleExample();
        VarHandle valueHandle = MethodHandles
            .privateLookupIn(VarHandleExample.class, MethodHandles.lookup())
            .findVarHandle(VarHandleExample.class, "value", int.class);

        // 预热
        for (int i = 0; i < WARMUP; i++) {
            valueHandle.getAndAdd(example, 1);
            example.atomicInt.getAndAdd(1);
        }

        // VarHandle 测试
        valueHandle.set(example, 0);
        long vhStart = System.nanoTime();
        CountDownLatch vhLatch = new CountDownLatch(THREADS);
        for (int t = 0; t < THREADS; t++) {
            new Thread(() -> {
                for (int i = 0; i < ITERATIONS; i++) {
                    valueHandle.getAndAdd(example, 1);
                }
                vhLatch.countDown();
            }).start();
        }
        vhLatch.await();
        long vhTime = System.nanoTime() - vhStart;
        int vhResult = (int) valueHandle.get(example);

        // AtomicInteger 测试
        example.atomicInt.set(0);
        long aiStart = System.nanoTime();
        CountDownLatch aiLatch = new CountDownLatch(THREADS);
        for (int t = 0; t < THREADS; t++) {
            new Thread(() -> {
                for (int i = 0; i < ITERATIONS; i++) {
                    example.atomicInt.getAndAdd(1);
                }
                aiLatch.countDown();
            }).start();
        }
        aiLatch.await();
        long aiTime = System.nanoTime() - aiStart;
        int aiResult = example.atomicInt.get();

        System.out.println("  VarHandle: 耗时=" + (vhTime / 1_000_000) + "ms, 结果=" + vhResult);
        System.out.println("  AtomicInteger: 耗时=" + (aiTime / 1_000_000) + "ms, 结果=" + aiResult);
        System.out.println("  性能对比: VarHandle " + String.format("%.2f", (double) aiTime / vhTime) + "x");
        System.out.println("  结论: VarHandle 和 AtomicInteger 性能相近");
    }

    // 8. VarHandle 模拟 AtomicReference
    private static void simulateAtomicReference() throws Exception {
        class SimpleAtomicReference<T> {
            private volatile T value;
            private final VarHandle valueHandle;

            public SimpleAtomicReference(T initialValue) {
                this.value = initialValue;
                try {
                    this.valueHandle = MethodHandles
                        .privateLookupIn(SimpleAtomicReference.class, MethodHandles.lookup())
                        .findVarHandle(SimpleAtomicReference.class, "value", Object.class);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public T get() {
                return (T) valueHandle.getVolatile(this);
            }

            public void set(T newValue) {
                valueHandle.setVolatile(this, newValue);
            }

            public boolean compareAndSet(T expect, T update) {
                return valueHandle.compareAndSet(this, expect, update);
            }

            public T getAndSet(T newValue) {
                return (T) valueHandle.getAndSet(this, newValue);
            }
        }

        SimpleAtomicReference<String> ref = new SimpleAtomicReference<>("初始值");

        System.out.println("  模拟 AtomicReference:");
        System.out.println("    初始值: " + ref.get());

        ref.set("更新值");
        System.out.println("    set后: " + ref.get());

        boolean success = ref.compareAndSet("更新值", "新值");
        System.out.println("    CAS(更新值->新值): " + success + ", 当前: " + ref.get());

        String old = ref.getAndSet("最后值");
        System.out.println("    getAndSet: 旧值=" + old + ", 新值=" + ref.get());
    }
}
