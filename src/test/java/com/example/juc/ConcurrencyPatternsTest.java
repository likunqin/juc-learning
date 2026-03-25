package com.example.juc;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并发设计模式单元测试
 */
class ConcurrencyPatternsTest {

    @Test
    void testProducerConsumerPattern() throws InterruptedException {
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(5);
        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);
        CountDownLatch doneLatch = new CountDownLatch(2);

        // 生产者
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    queue.put(i);
                    produced.incrementAndGet();
                }
                doneLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 消费者
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    queue.take();
                    consumed.incrementAndGet();
                }
                doneLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();

        doneLatch.await(5, TimeUnit.SECONDS);

        assertEquals(10, produced.get(), "生产者应该生产10个元素");
        assertEquals(10, consumed.get(), "消费者应该消费10个元素");
        assertTrue(queue.isEmpty(), "队列应该为空");
    }

    @Test
    void testGuardedSuspensionPattern() throws InterruptedException {
        class GuardedObject<T> {
            private T result;
            private volatile boolean ready = false;

            public synchronized T get() throws InterruptedException {
                while (!ready) {
                    wait();
                }
                return result;
            }

            public synchronized void set(T result) {
                this.result = result;
                this.ready = true;
                notifyAll();
            }
        }

        GuardedObject<String> guarded = new GuardedObject<>();
        AtomicInteger result = new AtomicInteger(0);
        CountDownLatch doneLatch = new CountDownLatch(2);

        Thread waiter = new Thread(() -> {
            try {
                String value = guarded.get();
                if (value.equals("success")) {
                    result.set(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        Thread setter = new Thread(() -> {
            try {
                Thread.sleep(500);
                guarded.set("success");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        waiter.start();
        setter.start();

        doneLatch.await(5, TimeUnit.SECONDS);
        assertEquals(1, result.get(), "Guarded Suspension 应该正确工作");
    }

    @Test
    void testBalkingPattern() {
        class BalkingJob {
            private volatile boolean started = false;

            public synchronized void startJob() {
                if (started) {
                    return; // 已经启动，拒绝执行
                }
                started = true;
            }

            public synchronized boolean isStarted() {
                return started;
            }
        }

        BalkingJob job = new BalkingJob();

        job.startJob();
        assertTrue(job.isStarted(), "第一次启动应该成功");

        job.startJob();
        // 第二次调用应该被拒绝，但不会抛出异常
        assertTrue(job.isStarted(), "第二次启动应该被拒绝");
    }

    @Test
    void testThreadPerMessagePattern() throws InterruptedException {
        AtomicInteger completed = new AtomicInteger(0);
        CountDownLatch doneLatch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    completed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        doneLatch.await(5, TimeUnit.SECONDS);
        assertEquals(5, completed.get(), "所有任务应该完成");
    }

    @Test
    void testWorkerThreadPattern() throws InterruptedException {
        class WorkerThreadPool {
            private final BlockingQueue<Runnable> taskQueue;
            private final Thread[] workers;
            private volatile boolean shutdown = false;

            public WorkerThreadPool(int poolSize, int queueSize) {
                this.taskQueue = new LinkedBlockingQueue<>(queueSize);
                this.workers = new Thread[poolSize];

                for (int i = 0; i < poolSize; i++) {
                    workers[i] = new Thread(() -> {
                        while (!shutdown || !taskQueue.isEmpty()) {
                            try {
                                Runnable task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                                if (task != null) {
                                    task.run();
                                }
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    });
                    workers[i].start();
                }
            }

            public void submit(Runnable task) throws InterruptedException {
                taskQueue.put(task);
            }

            public void shutdown() throws InterruptedException {
                shutdown = true;
                for (Thread worker : workers) {
                    worker.join();
                }
            }
        }

        WorkerThreadPool pool = new WorkerThreadPool(3, 10);
        AtomicInteger completed = new AtomicInteger(0);
        CountDownLatch doneLatch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            pool.submit(() -> {
                try {
                    Thread.sleep(100);
                    completed.incrementAndGet();
                    doneLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        doneLatch.await(5, TimeUnit.SECONDS);
        assertEquals(5, completed.get(), "所有任务应该完成");

        pool.shutdown();
    }

    @Test
    void testTwoPhaseTerminationPattern() throws InterruptedException {
        class TwoPhaseTerminationService {
            private Thread worker;
            private final AtomicInteger counter = new AtomicInteger(0);
            private volatile boolean shutdownRequested = false;

            public void start() {
                worker = new Thread(() -> {
                    while (!shutdownRequested) {
                        try {
                            Thread.sleep(100);
                            counter.incrementAndGet();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    // 第二阶段：清理
                    System.out.println("清理资源...");
                });
                worker.start();
            }

            public void requestShutdown() {
                shutdownRequested = true;
                worker.interrupt();
            }

            public void join() throws InterruptedException {
                worker.join();
            }

            public int getCounter() {
                return counter.get();
            }
        }

        TwoPhaseTerminationService service = new TwoPhaseTerminationService();
        service.start();

        Thread.sleep(500);
        service.requestShutdown();
        service.join();

        assertTrue(service.getCounter() > 0, "服务应该运行一段时间");
    }

    @Test
    void testFuturePromisePattern() throws InterruptedException {
        class Promise<T> {
            private T value;
            private volatile boolean ready = false;

            public synchronized void set(T value) {
                this.value = value;
                this.ready = true;
                notifyAll();
            }

            public synchronized T get() throws InterruptedException {
                while (!ready) {
                    wait();
                }
                return value;
            }

            public boolean isReady() {
                return ready;
            }
        }

        Promise<String> promise = new Promise<>();
        AtomicInteger result = new AtomicInteger(0);
        CountDownLatch doneLatch = new CountDownLatch(2);

        Thread waiter = new Thread(() -> {
            try {
                String value = promise.get();
                if (value.equals("completed")) {
                    result.set(1);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        Thread completer = new Thread(() -> {
            try {
                Thread.sleep(500);
                promise.set("completed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        waiter.start();
        completer.start();

        doneLatch.await(5, TimeUnit.SECONDS);
        assertEquals(1, result.get(), "Promise 模式应该正确工作");
    }

    @Test
    void testActiveObjectPattern() throws InterruptedException {
        class ActiveObject {
            private final BlockingQueue<Runnable> requestQueue;
            private final Thread dispatcher;

            public ActiveObject() {
                this.requestQueue = new LinkedBlockingQueue<>();
                this.dispatcher = new Thread(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Runnable request = requestQueue.take();
                            request.run();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
                dispatcher.start();
            }

            public void doSomething(Runnable callback) {
                requestQueue.add(callback);
            }

            public void shutdown() throws InterruptedException {
                dispatcher.interrupt();
                dispatcher.join();
            }
        }

        ActiveObject activeObject = new ActiveObject();
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch doneLatch = new CountDownLatch(5);

        for (int i = 0; i < 5; i++) {
            activeObject.doSomething(() -> {
                counter.incrementAndGet();
                doneLatch.countDown();
            });
        }

        doneLatch.await(5, TimeUnit.SECONDS);
        assertEquals(5, counter.get(), "所有请求应该被处理");

        activeObject.shutdown();
    }
}
