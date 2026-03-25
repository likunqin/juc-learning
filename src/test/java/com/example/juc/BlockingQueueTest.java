package com.example.juc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BlockingQueue 单元测试
 */
class BlockingQueueTest {

    @Test
    void testArrayBlockingQueuePutTake() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger result = new AtomicInteger();

        // 生产者
        new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    queue.put(i);
                }
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // 消费者
        new Thread(() -> {
            try {
                int sum = 0;
                for (int i = 0; i < 5; i++) {
                    sum += queue.take();
                }
                result.set(sum);
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(10, result.get(), "应该正确接收所有元素");
    }

    @Test
    void testLinkedBlockingQueueOfferPoll() throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(3);

        // offer 不阻塞
        assertTrue(queue.offer(1));
        assertTrue(queue.offer(2));
        assertTrue(queue.offer(3));

        // 队列满时 offer 返回 false
        assertFalse(queue.offer(4));

        // poll 不阻塞
        assertEquals(1, queue.poll());
        assertEquals(2, queue.poll());
        assertEquals(3, queue.poll()); // 取出第三个元素

        // 队列空时 poll 返回 null (poll 不阻塞，立即返回)
        assertNull(queue.poll());
        // 等待 10ms 再次确认
        assertNull(queue.poll(10, TimeUnit.MILLISECONDS));
    }

    @Test
    void testPriorityBlockingQueue() throws InterruptedException {
        BlockingQueue<Integer> queue = new PriorityBlockingQueue<>(5);

        // 添加元素顺序
        queue.put(3);
        queue.put(1);
        queue.put(5);
        queue.put(2);
        queue.put(4);

        // 取出应该是排序后的顺序
        assertEquals(1, queue.take());
        assertEquals(2, queue.take());
        assertEquals(3, queue.take());
        assertEquals(4, queue.take());
        assertEquals(5, queue.take());
    }

    @Test
    void testSynchronousQueue() throws InterruptedException {
        BlockingQueue<Integer> queue = new SynchronousQueue<>();
        CountDownLatch latch = new CountDownLatch(2);

        // 生产者
        new Thread(() -> {
            try {
                queue.put(1);
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // 消费者
        new Thread(() -> {
            try {
                int value = queue.take();
                assertEquals(1, value);
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        latch.await(5, TimeUnit.SECONDS);
        assertEquals(0, queue.remainingCapacity(), "SynchronousQueue 容量始终为 0");
    }

    @Test
    void testDrainTo() throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        for (int i = 0; i < 5; i++) {
            queue.put(i);
        }

        ConcurrentLinkedQueue<Integer> target = new ConcurrentLinkedQueue<>();
        int drained = queue.drainTo(target);

        assertEquals(5, drained, "应该全部转移");
        assertTrue(queue.isEmpty(), "源队列应该为空");
        assertEquals(5, target.size(), "目标队列应该有 5 个元素");
    }
}
