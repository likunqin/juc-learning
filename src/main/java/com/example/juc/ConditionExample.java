package com.example.juc;

import java.util.concurrent.locks.*;
import java.util.*;

/**
 * Condition 条件变量深度剖析示例
 * <p>
 * Condition 是 Lock 的条件变量，用于线程间协调，替代传统的 wait/notify/notifyAll。
 * </p>
 *
 * <h3>核心特性：</h3>
 * <ul>
 *   <li>绑定到 Lock 对象</li>
 *   <li>支持多个条件队列（一个 Lock 可创建多个 Condition）</li>
 *   <li>支持精确的等待/通知</li>
 *   <li>支持中断和超时</li>
 * </ul>
 *
 * <h3>主要方法：</h3>
 * <ul>
 *   <li>await() - 等待条件</li>
 *   <li>signal() - 唤醒一个等待线程</li>
   *   <li>signalAll() - 唤醒所有等待线程</li>
 *   <li>awaitNanos() / awaitUntil() - 带超时的等待</li>
 * </ul>
 *
 * <h3>vs Object.wait/notify：</h3>
 * <ul>
 *   <li>Condition 支持多条件队列</li>
 *   <li>Condition 可以指定公平性</li>
 *   <li>Condition 的 API 更清晰明确</li>
 * </ul>
 */
public class ConditionExample {

    /**
     * 场景1：生产者-消费者模式（单条件）
     * <p>
     * 使用 Condition 实现经典的生产者消费者模式
     * </p>
     */
    public static void producerConsumerSingleCondition() throws InterruptedException {
        System.out.println("=== 场景1：生产者-消费者（单条件） ===");

        class BoundedBuffer<T> {
            private final Queue<T> queue = new LinkedList<>();
            private final int capacity;
            private final Lock lock = new ReentrantLock();
            private final Condition notFull = lock.newCondition();
            private final Condition notEmpty = lock.newCondition();

            public BoundedBuffer(int capacity) {
                this.capacity = capacity;
            }

            public void put(T item) throws InterruptedException {
                lock.lock();
                try {
                    while (queue.size() == capacity) {
                        System.out.println("队列已满，生产者等待");
                        notFull.await();
                    }
                    queue.add(item);
                    System.out.println("生产: " + item + "，队列大小: " + queue.size());
                    notEmpty.signal();
                } finally {
                    lock.unlock();
                }
            }

            public T take() throws InterruptedException {
                lock.lock();
                try {
                    while (queue.isEmpty()) {
                        System.out.println("队列为空，消费者等待");
                        notEmpty.await();
                    }
                    T item = queue.remove();
                    System.out.println("消费: " + item + "，队列大小: " + queue.size());
                    notFull.signal();
                    return item;
                } finally {
                    lock.unlock();
                }
            }
        }

        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(3);
        CountDownLatch doneLatch = new CountDownLatch(10);
        Random random = new Random();

        // 生产者线程
        new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    buffer.put(i);
                    Thread.sleep(random.nextInt(300));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // 消费者线程
        new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    buffer.take();
                    Thread.sleep(random.nextInt(500));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        Thread.sleep(5000);
    }

    /**
     * 场景2：多个生产者消费者
     * <p>
     * 多个生产者和多个消费者并发操作
     * </p>
     */
    public static void multipleProducerConsumer() throws InterruptedException {
        System.out.println("\n=== 场景2：多生产者多消费者 ===");

        class SharedBuffer {
            private final Queue<String> queue = new LinkedList<>();
            private final int capacity = 5;
            private final Lock lock = new ReentrantLock();
            private final Condition notFull = lock.newCondition();
            private final Condition notEmpty = lock.newCondition();

            public void produce(String item, String producerName) throws InterruptedException {
                lock.lock();
                try {
                    while (queue.size() == capacity) {
                        notFull.await();
                    }
                    queue.add(item);
                    System.out.println(producerName + " 生产: " + item);
                    notEmpty.signal();
                } finally {
                    lock.unlock();
                }
            }

            public String consume(String consumerName) throws InterruptedException {
                lock.lock();
                try {
                    while (queue.isEmpty()) {
                        notEmpty.await();
                    }
                    String item = queue.remove();
                    System.out.println(consumerName + " 消费: " + item);
                    notFull.signal();
                    return item;
                } finally {
                    lock.unlock();
                }
            }
        }

        SharedBuffer buffer = new SharedBuffer();
        ExecutorService executor = Executors.newFixedThreadPool(6);

        // 3个生产者
        for (int i = 0; i < 3; i++) {
            final String name = "生产者" + (char) ('A' + i);
            final int start = i * 10;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 5; j++) {
                        buffer.produce("商品" + (start + j), name);
                        Thread.sleep(ThreadLocalRandom.current().nextInt(100, 300));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 3个消费者
        for (int i = 0; i < 3; i++) {
            final String name = "消费者" + (char) ('X' + i);
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 5; j++) {
                        buffer.consume(name);
                        Thread.sleep(ThreadLocalRandom.current().nextInt(150, 400));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        Thread.sleep(5000);
        executor.shutdown();
    }

    /**
     * 场景3：带超时的等待
     * <p>
     * 使用 awaitNanos 和 awaitUntil 实现超时等待
     * </p>
     */
    public static void awaitWithTimeout() throws InterruptedException {
        System.out.println("\n=== 场景3：带超时的等待 ===");

        class TimeoutBuffer {
            private boolean dataReady = false;
            private final Lock lock = new ReentrantLock();
            private final Condition condition = lock.newCondition();

            public boolean awaitData(long timeout, TimeUnit unit) throws InterruptedException {
                lock.lock();
                try {
                    long nanosTimeout = unit.toNanos(timeout);
                    while (!dataReady && nanosTimeout > 0) {
                        nanosTimeout = condition.awaitNanos(nanosTimeout);
                    }
                    return dataReady;
                } finally {
                    lock.unlock();
                }
            }

            public void setDataReady() {
                lock.lock();
                try {
                    dataReady = true;
                    condition.signal();
                } finally {
                    lock.unlock();
                }
            }
        }

        TimeoutBuffer buffer = new TimeoutBuffer();

        // 等待数据的线程（设置超时）
        Thread waiter = new Thread(() -> {
            try {
                System.out.println("等待线程开始等待数据（超时2秒）");
                boolean result = buffer.awaitData(2, TimeUnit.SECONDS);
                if (result) {
                    System.out.println("等待线程：数据已就绪！");
                } else {
                    System.out.println("等待线程：等待超时！");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        waiter.start();

        // 3秒后才设置数据（故意超时）
        Thread.sleep(3000);
        buffer.setDataReady();

        Thread.sleep(500);
    }

    /**
     * 场景4：使用 signalAll
     * <p>
     * 演示 signal 和 signalAll 的区别
     * </p>
     */
    public static void signalVsSignalAll() throws InterruptedException {
        System.out.println("\n=== 场景4：signal vs signalAll ===");

        class ResourcePool {
            private int available = 0;
            private final Lock lock = new ReentrantLock();
            private final Condition condition = lock.newCondition();

            public void waitResource(int id, boolean useSignalAll) throws InterruptedException {
                lock.lock();
                try {
                    System.out.println("线程 " + id + " 等待资源");
                    condition.await();
                    System.out.println("线程 " + id + " 获取到资源，开始工作");
                } finally {
                    lock.unlock();
                }
            }

            public void releaseResource(boolean useSignalAll) {
                lock.lock();
                try {
                    if (useSignalAll) {
                        System.out.println("唤醒所有等待线程");
                        condition.signalAll();
                    } else {
                        System.out.println("唤醒一个等待线程");
                        condition.signal();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        ResourcePool pool = new ResourcePool();

        // 创建5个等待线程
        for (int i = 0; i < 5; i++) {
            final int id = i;
            new Thread(() -> {
                try {
                    pool.waitResource(id, true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        Thread.sleep(500);
        pool.releaseResource(true); // 使用 signalAll

        Thread.sleep(2000);
    }

    /**
     * 场景5：阻塞队列实现
     * <p>
     * 使用 Condition 实现一个简单的阻塞队列
     * </p>
     */
    public static void blockingQueueImplementation() throws InterruptedException {
        System.out.println("\n=== 场景5：阻塞队列实现 ===");

        class MyBlockingQueue<T> {
            private final Queue<T> queue = new LinkedList<>();
            private final int capacity;
            private final Lock lock = new ReentrantLock();
            private final Condition notFull = lock.newCondition();
            private final Condition notEmpty = lock.newCondition();

            public MyBlockingQueue(int capacity) {
                this.capacity = capacity;
            }

            public void put(T item) throws InterruptedException {
                lock.lock();
                try {
                    while (queue.size() == capacity) {
                        notFull.await();
                    }
                    queue.offer(item);
                    notEmpty.signal();
                } finally {
                    lock.unlock();
                }
            }

            public T take() throws InterruptedException {
                lock.lock();
                try {
                    while (queue.isEmpty()) {
                        notEmpty.await();
                    }
                    T item = queue.poll();
                    notFull.signal();
                    return item;
                } finally {
                    lock.unlock();
                }
            }

            public int size() {
                lock.lock();
                try {
                    return queue.size();
                } finally {
                    lock.unlock();
                }
            }
        }

        MyBlockingQueue<Integer> queue = new MyBlockingQueue<>(3);

        // 生产者
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    queue.put(i);
                    System.out.println("生产: " + i + "，当前队列大小: " + queue.size());
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 消费者
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    int item = queue.take();
                    System.out.println("消费: " + item + "，当前队列大小: " + queue.size());
                    Thread.sleep(350);
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
     * 场景6：读写锁中的 Condition
     * <p>
     * 演示在 ReadWriteLock 中使用 Condition
     * </p>
     */
    public static void conditionInReadWriteLock() throws InterruptedException {
        System.out.println("\n=== 场景6：读写锁中的 Condition ===");

        class ReadWriteCache {
            private String data = "初始数据";
            private boolean dataDirty = false;
            private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
            private final Lock writeLock = rwLock.writeLock();
            private final Condition dataClean = writeLock.newCondition();

            public String read() {
                rwLock.readLock().lock();
                try {
                    System.out.println("读取数据: " + data);
                    return data;
                } finally {
                    rwLock.readLock().unlock();
                }
            }

            public void write(String newData) throws InterruptedException {
                writeLock.lock();
                try {
                    System.out.println("等待数据清理...");
                    while (dataDirty) {
                        dataClean.await();
                    }
                    System.out.println("写入数据: " + newData);
                    data = newData;
                } finally {
                    writeLock.unlock();
                }
            }

            public void markDirty() {
                writeLock.lock();
                try {
                    dataDirty = true;
                    System.out.println("标记数据为脏");
                } finally {
                    writeLock.unlock();
                }
            }

            public void markClean() {
                writeLock.lock();
                try {
                    dataDirty = false;
                    dataClean.signalAll();
                    System.out.println("数据已清理，通知写线程");
                } finally {
                    writeLock.unlock();
                }
            }
        }

        ReadWriteCache cache = new ReadWriteCache();

        cache.read();
        cache.read();

        Thread writer = new Thread(() -> {
            try {
                cache.write("新数据");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread cleaner = new Thread(() -> {
            try {
                Thread.sleep(500);
                cache.markClean();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        cache.markDirty();
        writer.start();
        cleaner.start();

        writer.join();
        cleaner.join();
    }

    /**
     * 场景7：多个条件队列
     * <p>
     * 一个 Lock 创建多个 Condition，实现不同的等待条件
     * </p>
     */
    public static void multipleConditions() throws InterruptedException {
        System.out.println("\n=== 场景7：多个条件队列 ===");

        class AdvancedBuffer<T> {
            private final Queue<T> queue = new LinkedList<>();
            private final int capacity = 5;
            private final Lock lock = new ReentrantLock();
            private final Condition notFull = lock.newCondition(); // 等待非满条件
            private final Condition notEmpty = lock.newCondition(); // 等待非空条件
            private final Condition lowWaterMark = lock.newCondition(); // 低水位条件

            public void put(T item) throws InterruptedException {
                lock.lock();
                try {
                    while (queue.size() >= capacity) {
                        System.out.println("队列满，等待 notFull");
                        notFull.await();
                    }
                    queue.add(item);
                    System.out.println("放入: " + item + "，大小: " + queue.size());

                    // 如果达到低水位，通知
                    if (queue.size() >= 3) {
                        System.out.println("达到低水位3，通知 lowWaterMark");
                        lowWaterMark.signalAll();
                    }

                    notEmpty.signal();
                } finally {
                    lock.unlock();
                }
            }

            public T take() throws InterruptedException {
                lock.lock();
                try {
                    while (queue.isEmpty()) {
                        System.out.println("队列空，等待 notEmpty");
                        notEmpty.await();
                    }
                    T item = queue.remove();
                    System.out.println("取出: " + item + "，大小: " + queue.size());
                    notFull.signal();
                    return item;
                } finally {
                    lock.unlock();
                }
            }

            public void waitForLowWaterMark() throws InterruptedException {
                lock.lock();
                try {
                    while (queue.size() < 3) {
                        System.out.println("等待队列达到3个元素");
                        lowWaterMark.await();
                    }
                    System.out.println("队列已达到3个元素");
                } finally {
                    lock.unlock();
                }
            }
        }

        AdvancedBuffer<String> buffer = new AdvancedBuffer<>();

        // 等待低水位的线程
        new Thread(() -> {
            try {
                buffer.waitForLowWaterMark();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // 消费者
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 3; i++) {
                    buffer.take();
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 生产者
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    buffer.put("商品" + i);
                    Thread.sleep(300);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        consumer.start();
        Thread.sleep(200);
        producer.start();

        producer.join();
        consumer.join();
    }

    /**
     * 最佳实践与注意事项
     */
    public static void bestPractices() {
        System.out.println("\n=== 最佳实践与注意事项 ===");

        System.out.println("""
        1. 永远在持有锁时调用 Condition 方法
           - await()、signal()、signalAll() 必须在 lock.lock() 之后
           - 否则会抛出 IllegalMonitorStateException

        2. await() 建议使用 while 循环检查条件
           - 避免"虚假唤醒"（spurious wakeup）
           - 即使被唤醒也要重新检查条件

        3. lock.unlock() 必须在 finally 中
           - 确保锁一定会被释放
           - 避免死锁

        4. 区分 signal 和 signalAll
           - signal: 只唤醒一个等待线程
           - signalAll: 唤醒所有等待线程
           - 根据业务场景选择

        5. 避免在锁内执行耗时操作
           - await() 会释放锁，但持有锁期间不应做耗时操作
           - 会影响其他线程的执行

        6. 正确处理 InterruptedException
           - await() 可能被中断
           - 被中断时应恢复中断状态

        7. 合理使用多个 Condition
           - 不同条件使用不同的 Condition
           - 可以减少不必要的唤醒，提高效率

        8. awaitUntil 使用场景
           - 需要在特定时间点之前完成等待
           - 可以用于超时场景

        9. Condition vs wait/notify
           - Condition 功能更强大，推荐使用
           - 但简单场景 wait/notify 也可以
        """);
    }

    public static void main(String[] args) throws InterruptedException {
        producerConsumerSingleCondition();
        multipleProducerConsumer();
        awaitWithTimeout();
        signalVsSignalAll();
        blockingQueueImplementation();
        conditionInReadWriteLock();
        multipleConditions();
        bestPractices();
    }
}
