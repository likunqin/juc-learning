package com.example.juc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 锁机制深度剖析
 * 学习各种锁的实现原理和使用场景
 */
public class LockDeepDive {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 锁机制深度剖析 ===");

        // 1. ReentrantLock基础使用
        System.out.println("\n1. ReentrantLock基础:");
        ReentrantLock lock = new ReentrantLock();

        lock.lock();
        try {
            System.out.println("获取锁成功");
            System.out.println("是否持有锁: " + lock.isHeldByCurrentThread());
            System.out.println("锁的持有计数: " + lock.getHoldCount());

            // 可重入演示
            lock.lock();
            try {
                System.out.println("再次获取锁 - 可重入");
                System.out.println("锁的持有计数: " + lock.getHoldCount());
            } finally {
                lock.unlock();
            }

            System.out.println("解锁一次后持有计数: " + lock.getHoldCount());
        } finally {
            lock.unlock();
        }

        System.out.println("最终持有计数: " + lock.getHoldCount());

        // 2. 公平锁vs非公平锁
        System.out.println("\n2. 公平锁 vs 非公平锁:");

        // 公平锁 - 按申请顺序获取
        ReentrantLock fairLock = new ReentrantLock(true);
        System.out.println("公平锁: " + fairLock.isFair());

        // 非公平锁 - 可能插队
        ReentrantLock unfairLock = new ReentrantLock(false);
        System.out.println("非公平锁: " + unfairLock.isFair());

        // 3. tryLock的各种用法
        System.out.println("\n3. tryLock用法:");
        ReentrantLock tryLock = new ReentrantLock();

        // 非阻塞尝试获取锁
        boolean acquired = tryLock.tryLock();
        System.out.println("tryLock() 立即尝试: " + acquired);

        if (acquired) {
            try {
                System.out.println("获取锁成功");

                // 模拟其他线程尝试获取锁
                Thread otherThread = new Thread(() -> {
                    boolean blocked = tryLock.tryLock();
                    System.out.println("其他线程tryLock(): " + blocked);

                    if (!blocked) {
                        // 尝试带超时的获取
                        try {
                            boolean timed = tryLock.tryLock(2, TimeUnit.SECONDS);
                            System.out.println("其他线程tryLock(2s): " + timed);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });

                otherThread.start();
                Thread.sleep(1000); // 让其他线程有时间执行
                otherThread.join();

            } finally {
                tryLock.unlock();
            }
        }

        // 4. lockInterruptibly
        System.out.println("\n4. lockInterruptibly (可中断锁):");
        ReentrantLock interruptLock = new ReentrantLock();

        Thread interruptedThread = new Thread(() -> {
            try {
                interruptLock.lockInterruptibly();
                try {
                    System.out.println("线程获取锁，准备长时间执行...");
                    Thread.sleep(5000); // 长时间执行
                } finally {
                    interruptLock.unlock();
                }
            } catch (InterruptedException e) {
                System.out.println("线程被中断，放弃获取锁");
                Thread.currentThread().interrupt();
            }
        });

        interruptLock.lock(); // 主线程先获取锁
        interruptedThread.start();
        Thread.sleep(100); // 让子线程开始尝试获取锁

        interruptedThread.interrupt(); // 中断子线程
        Thread.sleep(100); // 等待中断处理

        interruptLock.unlock(); // 释放锁
        interruptedThread.join();

        // 5. Condition使用
        System.out.println("\n5. Condition (条件变量):");
        ReentrantLock conditionLock = new ReentrantLock();
        Condition condition = conditionLock.newCondition();

        Thread waiter = new Thread(() -> {
            conditionLock.lock();
            try {
                System.out.println("等待线程: 等待条件...");
                condition.await(); // 等待
                System.out.println("等待线程: 条件满足，继续执行");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                conditionLock.unlock();
            }
        });

        Thread signaler = new Thread(() -> {
            try {
                Thread.sleep(1000); // 模拟一些工作
                conditionLock.lock();
                try {
                    System.out.println("通知线程: 发送信号");
                    condition.signal(); // 发送信号
                } finally {
                    conditionLock.unlock();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        waiter.start();
        signaler.start();
        waiter.join();
        signaler.join();

        // 6. ReadWriteLock深度使用
        System.out.println("\n6. ReadWriteLock深度使用:");
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
        ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

        // 读写锁状态查询
        System.out.println("读锁数量: " + rwLock.getReadLockCount());
        System.out.println("写锁是否被持有: " + rwLock.isWriteLocked());
        System.out.println("写锁被当前线程持有: " + rwLock.isWriteLockedByCurrentThread());

        // 读锁降级为写锁
        System.out.println("\n读锁降级为写锁:");
        readLock.lock();
        try {
            System.out.println("获取读锁");

            // 读锁降级：先获取写锁，再释放读锁
            writeLock.lock();
            try {
                System.out.println("在读锁保护下获取写锁（降级）");
                // 这里可以安全地进行写操作
            } finally {
                readLock.unlock(); // 释放读锁
                System.out.println("释放读锁，完成降级");
            }
        } finally {
            // 注意：这里不能释放读锁，因为前面已经释放了
            if (rwLock.getReadLockCount() > 0) {
                readLock.unlock();
            }
        }

        // 7. StampedLock介绍（Java 8+）
        System.out.println("\n7. StampedLock (Java 8+):");
        StampedLock stampedLock = new StampedLock();

        // 乐观读
        long stamp = stampedLock.tryOptimisticRead();
        if (!stampedLock.validate(stamp)) {
            // 乐观读失败，转为悲观读
            stamp = stampedLock.readLock();
            try {
                System.out.println("乐观读失败，使用悲观读");
            } finally {
                stampedLock.unlockRead(stamp);
            }
        } else {
            System.out.println("乐观读成功");
        }

        // 写锁
        long writeStamp = stampedLock.writeLock();
        try {
            System.out.println("获取StampedLock写锁");
        } finally {
            stampedLock.unlockWrite(writeStamp);
        }

        // 8. 锁的性能对比概念
        System.out.println("\n8. 锁性能对比概念:");

        System.out.println("synchronized:");
        System.out.println("  - JVM内置，使用简单");
        System.out.println("  - 自适应锁优化");
        System.out.println("  - 无法中断，无法超时");

        System.out.println("ReentrantLock:");
        System.out.println("  - 可中断，可超时");
        System.out.println("  - 公平锁选项");
        System.out.println("  - 需要手动释放");

        System.out.println("StampedLock:");
        System.out.println("  - 乐观读优化");
        System.out.println("  - 不支持条件变量");
        System.out.println("  - 性能更高");

        // 9. 死锁检测
        System.out.println("\n9. 死锁预防示例:");
        ReentrantLock lock1 = new ReentrantLock();
        ReentrantLock lock2 = new ReentrantLock();

        // 正确的锁顺序
        Thread thread1 = new Thread(() -> {
            if (lock1.tryLock()) {
                try {
                    Thread.sleep(100);
                    if (lock2.tryLock()) {
                        try {
                            System.out.println("线程1成功获取两个锁");
                        } finally {
                            lock2.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock1.unlock();
                }
            }
        });

        Thread thread2 = new Thread(() -> {
            if (lock1.tryLock()) { // 使用相同顺序
                try {
                    if (lock2.tryLock()) {
                        try {
                            System.out.println("线程2成功获取两个锁");
                        } finally {
                            lock2.unlock();
                        }
                    }
                } finally {
                    lock1.unlock();
                }
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        System.out.println("使用相同锁顺序避免死锁");
    }
}