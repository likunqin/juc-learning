package com.example.juc;

import java.util.concurrent.atomic.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Atomic类深度剖析
 * 学习CAS原理和原子操作的实现机制
 */
public class AtomicDeepDive {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Atomic类深度剖析 ===");

        // 1. CAS原理演示
        System.out.println("\n1. CAS (Compare And Swap) 原理:");
        AtomicInteger atomicInt = new AtomicInteger(0);
        System.out.println("初始值: " + atomicInt.get());

        // 模拟CAS操作
        int expectedValue = 0;
        int newValue = 100;
        boolean success = atomicInt.compareAndSet(expectedValue, newValue);
        System.out.println("CAS操作 (0 -> 100): " + success + ", 当前值: " + atomicInt.get());

        // CAS失败的情况
        expectedValue = 50; // 期望值错误
        newValue = 200;
        success = atomicInt.compareAndSet(expectedValue, newValue);
        System.out.println("CAS操作 (50 -> 200, 但当前是100): " + success + ", 当前值: " + atomicInt.get());

        // 2. 各种原子操作
        System.out.println("\n2. 原子操作类型:");

        // 基本原子操作
        System.out.println("初始值: " + atomicInt.get());
        System.out.println("getAndIncrement(): " + atomicInt.getAndIncrement() + ", 新值: " + atomicInt.get());
        System.out.println("incrementAndGet(): " + atomicInt.incrementAndGet() + ", 当前值: " + atomicInt.get());
        System.out.println("getAndDecrement(): " + atomicInt.getAndDecrement() + ", 新值: " + atomicInt.get());
        System.out.println("decrementAndGet(): " + atomicInt.decrementAndGet() + ", 当前值: " + atomicInt.get());

        // 带delta的操作
        System.out.println("addAndGet(10): " + atomicInt.addAndGet(10) + ", 当前值: " + atomicInt.get());
        System.out.println("getAndAdd(5): " + atomicInt.getAndAdd(5) + ", 新值: " + atomicInt.get());

        // 更新操作
        System.out.println("updateAndGet(x -> x * 2): " + atomicInt.updateAndGet(x -> x * 2));
        System.out.println("accumulateAndGet(3, (x, y) -> x + y): " + atomicInt.accumulateAndGet(3, (x, y) -> x + y));

        // 3. 原子更新器
        System.out.println("\n3. 原子更新器:");
        User user = new User("张三", 25);
        System.out.println("初始用户: " + user);

        // AtomicReferenceFieldUpdater - 更新引用字段
        AtomicReferenceFieldUpdater<User, String> nameUpdater =
                AtomicReferenceFieldUpdater.newUpdater(User.class, String.class, "name");

        nameUpdater.compareAndSet(user, "张三", "李四");
        System.out.println("更新姓名后: " + user);

        // AtomicIntegerFieldUpdater - 更新int字段
        AtomicIntegerFieldUpdater<User> ageUpdater =
                AtomicIntegerFieldUpdater.newUpdater(User.class, "age");

        ageUpdater.incrementAndGet(user);
        System.out.println("年龄+1后: " + user);

        // 4. 原子累加器（高性能）
        System.out.println("\n4. 原子累加器 (Striped64):");

        // LongAdder - 高并发下性能更好
        LongAdder adder = new LongAdder();
        System.out.println("LongAdder初始值: " + adder.sum());

        Thread[] threads = new Thread[10];
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10000; j++) {
                    adder.increment();
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        long endTime = System.currentTimeMillis();
        System.out.println("LongAdder最终值: " + adder.sum() + ", 耗时: " + (endTime - startTime) + "ms");

        // 对比AtomicLong
        AtomicLong atomicLong = new AtomicLong(0);
        startTime = System.currentTimeMillis();

        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10000; j++) {
                    atomicLong.incrementAndGet();
                }
            });
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        endTime = System.currentTimeMillis();
        System.out.println("AtomicLong最终值: " + atomicLong.get() + ", 耗时: " + (endTime - startTime) + "ms");

        // 5. AtomicBoolean和AtomicReference
        System.out.println("\n5. 其他原子类:");

        // AtomicBoolean
        AtomicBoolean flag = new AtomicBoolean(false);
        System.out.println("AtomicBoolean初始: " + flag.get());
        System.out.println("compareAndSet(false, true): " + flag.compareAndSet(false, true));
        System.out.println("当前值: " + flag.get());

        // AtomicReference
        AtomicReference<User> userRef = new AtomicReference<>(new User("王五", 30));
        System.out.println("AtomicReference初始: " + userRef.get());

        User newUser = new User("赵六", 35);
        User oldUser = userRef.getAndSet(newUser);
        System.out.println("getAndSet旧值: " + oldUser + ", 新值: " + userRef.get());

        // 6. 原子操作的应用场景
        System.out.println("\n6. 应用场景示例:");

        // 计数器
        AtomicInteger counter = new AtomicInteger(0);
        System.out.println("线程安全计数器: " + counter.incrementAndGet());

        // 状态标志
        AtomicBoolean shutdown = new AtomicBoolean(false);
        System.out.println("服务关闭标志: " + shutdown.compareAndSet(false, true));

        // 缓存版本号
        AtomicLong version = new AtomicLong(1L);
        System.out.println("缓存版本号: " + version.incrementAndGet());

        // 7. ABA问题演示
        System.out.println("\n7. ABA问题演示:");
        AtomicReference<String> abaRef = new AtomicReference<>("A");
        System.out.println("初始值: " + abaRef.get());

        // 线程1: A -> B
        abaRef.compareAndSet("A", "B");
        System.out.println("A -> B: " + abaRef.get());

        // 线程2: B -> A
        abaRef.compareAndSet("B", "A");
        System.out.println("B -> A: " + abaRef.get());

        // 线程3: 尝试 A -> C，但不知道中间发生了变化
        boolean abaSuccess = abaRef.compareAndSet("A", "C");
        System.out.println("A -> C 成功?: " + abaSuccess + ", 当前值: " + abaRef.get());

        // 使用AtomicStampedReference解决ABA问题
        System.out.println("\n使用AtomicStampedReference解决ABA:");
        AtomicStampedReference<String> stampedRef = new AtomicStampedReference<>("A", 0);
        System.out.println("初始值: " + stampedRef.getReference() + ", 版本: " + stampedRef.getStamp());

        // A -> B，版本+1
        int[] stampHolder = new int[1];
        String currentValue = stampedRef.get(stampHolder);
        stampedRef.compareAndSet(currentValue, "B", stampHolder[0], stampHolder[0] + 1);
        System.out.println("A -> B: " + stampedRef.getReference() + ", 版本: " + stampedRef.getStamp());

        // B -> A，版本+1
        currentValue = stampedRef.get(stampHolder);
        stampedRef.compareAndSet(currentValue, "A", stampHolder[0], stampHolder[0] + 1);
        System.out.println("B -> A: " + stampedRef.getReference() + ", 版本: " + stampedRef.getStamp());

        // 尝试 A -> C，但版本不匹配，会失败
        currentValue = stampedRef.get(stampHolder);
        success = stampedRef.compareAndSet(currentValue, "C", 0, 1); // 使用旧版本号
        System.out.println("A -> C (使用旧版本): " + success + ", 当前值: " + stampedRef.getReference());
    }

    // 用户类用于演示
    static class User {
        volatile String name;
        volatile int age;

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "User{name='" + name + "', age=" + age + '}';
        }
    }
}