package com.example.juc;

import java.util.concurrent.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发设计模式学习示例
 * 生产者-消费者、Future、Guarded Suspension、Balking等模式
 */
public class ConcurrencyPatterns {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 并发设计模式学习示例 ===\n");

        // 1. 生产者-消费者模式
        System.out.println("1. 生产者-消费者模式:");
        producerConsumer();

        // 2. Guarded Suspension 模式
        System.out.println("\n2. Guarded Suspension 模式:");
        guardedSuspension();

        // 3. Balking 模式
        System.out.println("\n3. Balking 模式:");
        balkingPattern();

        // 4. Promise 模式 (Future)
        System.out.println("\n4. Promise 模式:");
        promisePattern();

        // 5. Thread-Per-Message 模式
        System.out.println("\n5. Thread-Per-Message 模式:");
        threadPerMessage();

        // 6. Worker Thread 模式
        System.out.println("\n6. Worker Thread 模式:");
        workerThread();

        // 7. Two-Phase Termination 模式
        System.out.println("\n7. Two-Phase Termination 模式:");
        twoPhaseTermination();
    }

    // 1. 生产者-消费者模式
    private static void producerConsumer() throws InterruptedException {
        BlockingQueue<Integer> queue = new ArrayBlockingQueue<>(5);

        // 生产者
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    queue.put(i);
                    System.out.println("  生产: " + i + ", 队列大小: " + queue.size());
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // 消费者
        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    Integer item = queue.take();
                    System.out.println("  消费: " + item + ", 队列大小: " + queue.size());
                    Thread.sleep(150);

                    if (item == 10) break;
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

    // 2. Guarded Suspension 模式
    // 条件不满足时等待，满足后继续执行
    private static void guardedSuspension() throws InterruptedException {
        RequestQueue queue = new RequestQueue();

        // 消费者线程 - 等待请求
        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                Request request = queue.get();
                System.out.println("  处理请求: " + request);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        // 生产者线程 - 延迟添加请求
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                Request request = new Request("请求" + i);
                queue.put(request);
                System.out.println("  添加请求: " + request);
            }
        });

        consumer.start();
        producer.start();
        producer.join();
        consumer.join();
    }

    // 请求队列
    static class RequestQueue {
        private final LinkedList<Request> queue = new LinkedList<>();

        public synchronized Request get() {
            while (queue.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return queue.removeFirst();
        }

        public synchronized void put(Request request) {
            queue.addLast(request);
            notifyAll();
        }
    }

    static class Request {
        final String name;

        public Request(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    // 3. Balking 模式
    // 条件不满足时不执行，直接返回
    private static void balkingPattern() {
        Data data = new Data("初始数据");
        AtomicInteger counter = new AtomicInteger(0);

        // 多个线程尝试修改数据
        Thread[] threads = new Thread[5];
        for (int i = 0; i < threads.length; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                boolean saved = data.save("修改" + threadId);
                if (saved) {
                    counter.incrementAndGet();
                    System.out.println("  线程" + threadId + " 保存成功");
                } else {
                    System.out.println("  线程" + threadId + " 跳过保存");
                }
            });
        }

        for (Thread t : threads) t.start();
        try {
            for (Thread t : threads) t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("  成功保存次数: " + counter.get() + "/5");
    }

    // 数据类
    static class Data {
        private String content;
        private boolean changed = false;

        public Data(String content) {
            this.content = content;
        }

        public synchronized boolean save(String newContent) {
            if (!changed) {
                this.content = newContent;
                this.changed = true;
                return true;
            }
            return false; // 已被修改，跳过
        }
    }

    // 4. Promise 模式 (Future)
    private static void promisePattern() throws Exception {
        System.out.println("  使用Promise模式获取异步结果:");

        PromiseExecutor executor = new PromiseExecutor();

        // 提交异步任务
        Promise<String> promise = executor.execute(() -> {
            System.out.println("    异步任务执行中...");
            Thread.sleep(1000);
            return "任务完成";
        });

        System.out.println("  主线程可以继续做其他工作...");
        Thread.sleep(500);

        // 获取结果（会阻塞直到完成）
        String result = promise.get();
        System.out.println("  获取结果: " + result);
    }

    // Promise接口
    interface Promise<T> {
        T get() throws InterruptedException, ExecutionException;
        boolean isDone();
    }

    // Promise执行器
    static class PromiseExecutor {
        private final ExecutorService executor = Executors.newCachedThreadPool();

        public <T> Promise<T> execute(Callable<T> task) {
            CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executor);

            return new Promise<T>() {
                @Override
                public T get() throws InterruptedException, ExecutionException {
                    return future.get();
                }

                @Override
                public boolean isDone() {
                    return future.isDone();
                }
            };
        }
    }

    // 5. Thread-Per-Message 模式
    // 每个消息/请求分配一个新线程
    private static void threadPerMessage() {
        MessageHandler handler = new MessageHandler();

        // 处理多条消息
        for (int i = 1; i <= 5; i++) {
            final int messageId = i;
            handler.handle("消息" + messageId);
            System.out.println("  已提交消息" + messageId);
        }

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 消息处理器
    static class MessageHandler {
        public void handle(String message) {
            new Thread(() -> {
                System.out.println("    处理: " + message);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("    完成: " + message);
            }).start();
        }
    }

    // 6. Worker Thread 模式
    // 固定数量的工作线程处理任务
    private static void workerThread() throws InterruptedException {
        WorkerPool pool = new WorkerPool(3); // 3个工作线程

        // 提交任务
        for (int i = 1; i <= 6; i++) {
            final int taskId = i;
            pool.submit(() -> {
                System.out.println("    工作线程处理任务" + taskId);
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("    任务" + taskId + " 完成");
            });
            Thread.sleep(100);
        }

        Thread.sleep(3000);
        pool.shutdown();
    }

    // 工作线程池
    static class WorkerPool {
        private final BlockingQueue<Runnable> queue;
        private final List<Worker> workers;

        public WorkerPool(int poolSize) {
            this.queue = new LinkedBlockingQueue<>();
            this.workers = new ArrayList<>(poolSize);

            for (int i = 0; i < poolSize; i++) {
                Worker worker = new Worker("Worker-" + i);
                workers.add(worker);
                worker.start();
            }
            System.out.println("  创建了" + poolSize + "个工作线程");
        }

        public void submit(Runnable task) {
            queue.offer(task);
        }

        public void shutdown() {
            for (Worker worker : workers) {
                worker.interrupt();
            }
        }

        class Worker extends Thread {
            public Worker(String name) {
                super(name);
            }

            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Runnable task = queue.take();
                        task.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    // 7. Two-Phase Termination 模式
    // 两阶段终止：先请求停止，再实际停止
    private static void twoPhaseTermination() throws InterruptedException {
        Service service = new Service();

        // 启动服务
        service.start();

        // 运行一会儿
        Thread.sleep(2000);

        // 请求停止
        System.out.println("  请求服务停止...");
        service.stop();

        // 等待完全停止
        while (!service.isStopped()) {
            Thread.sleep(100);
        }
        System.out.println("  服务已完全停止");
    }

    // 服务类
    static class Service {
        private volatile boolean shutdownRequested = false;
        private Thread workerThread;

        public void start() {
            workerThread = new Thread(() -> {
                while (!shutdownRequested) {
                    System.out.println("    服务运行中...");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // 检查是否是停止请求
                        if (shutdownRequested) {
                            break;
                        }
                    }
                }
                System.out.println("    服务线程结束");
                // 执行清理工作
                cleanup();
            });
            workerThread.start();
        }

        public void stop() {
            shutdownRequested = true;
            workerThread.interrupt(); // 唤醒可能阻塞的线程
        }

        public boolean isStopped() {
            return !workerThread.isAlive();
        }

        private void cleanup() {
            System.out.println("    执行清理工作...");
        }
    }
}
