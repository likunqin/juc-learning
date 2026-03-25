# JUC学习项目

这是一个用于学习Java并发编程（JUC - java.util.concurrent）的源码项目。

## 项目结构

```
src/main/java/com/example/juc/
├── ThreadBasicExample.java         # 线程基础 Thread/Runnable/Callable
├── AtomicExample.java              # Atomic原子类基础示例
├── ThreadPoolExample.java          # 线程池基础示例
├── ThreadPoolBestPractices.java    # 线程池最佳实践
├── SynchronizationExample.java     # 同步工具类基础示例
├── ConcurrentCollectionsExample.java  # 并发集合基础示例
├── CompletableFutureExample.java   # CompletableFuture异步编程
├── CompletableFutureAdvanced.java # CompletableFuture进阶
├── ThreadLocalExample.java        # ThreadLocal线程本地变量
├── FutureExample.java             # Future/FutureTask异步任务
├── VolatileExample.java           # Volatile关键字深度解析
├── SynchronizedDeepDive.java      # Synchronized关键字深度解析
├── ExchangerExample.java          # Exchanger数据交换
├── PhaserExample.java             # Phaser高级同步器
├── DelayQueueExample.java         # DelayQueue延迟队列
├── TransferQueueExample.java      # TransferQueue传输队列
├── PriorityBlockingQueueExample.java  # PriorityBlockingQueue优先级队列
├── SynchronousQueueExample.java   # SynchronousQueue同步队列
├── CopyOnWriteArraySetExample.java  # CopyOnWriteArraySet写时复制Set
├── ConcurrentSkipListMapExample.java  # ConcurrentSkipListMap有序并发Map
├── ForkJoinPoolDeepDive.java     # ForkJoinPool工作窃取算法
├── ConcurrencyPatterns.java       # 并发设计模式
├── AtomicDeepDive.java            # Atomic类深度剖析
├── BlockingQueueDeepDive.java     # BlockingQueue深度剖析
├── ConcurrentHashMapDeepDive.java # ConcurrentHashMap深度剖析
├── LockDeepDive.java              # 锁机制深度剖析
├── StampedLockExample.java        # StampedLock乐观读锁
├── VarHandleExample.java          # JDK 9+ VarHandle原子操作
├── ConcurrencyOptimization.java   # 并发性能优化技巧
└── VirtualThreadExample.java       # Java 21+ 虚拟线程
```

## 学习内容

### 基础示例

1. **线程基础** - Thread、Runnable、Callable、线程状态
2. **Atomic类** - 原子操作基础
3. **线程池** - 各种线程池使用
4. **同步工具类** - 锁和同步器基础
5. **并发集合** - 线程安全集合基础
6. **CompletableFuture** - 异步编程核心API
7. **ThreadLocal** - 线程本地变量
8. **Future/FutureTask** - 异步任务基础
9. **Volatile** - 可见性、有序性、非原子性
10. **Synchronized** - 深度解析锁升级机制
11. **Exchanger** - 线程间数据交换
12. **Phaser** - 高级多阶段同步器
13. **DelayQueue** - 延迟队列实现
14. **TransferQueue** - 手递手模式传输队列
15. **PriorityBlockingQueue** - 优先级阻塞队列
16. **SynchronousQueue** - 同步队列（零容量）
17. **CopyOnWriteArraySet** - 写时复制Set
18. **ConcurrentSkipListMap** - 有序并发Map
19. **StampedLock** - 乐观读锁与读写锁性能对比
20. **VarHandle** - JDK 9+ 原子操作新方式
21. **虚拟线程** - Java 21+ 轻量级线程
22. **并发优化** - 性能优化最佳实践

### 深度剖析示例

#### AtomicDeepDive.java
- **CAS原理** - Compare And Swap机制
- **原子操作类型** - increment, add, update, accumulate
- **原子更新器** - FieldUpdater的使用
- **LongAdder** - 高性能累加器
- **ABA问题** - 及AtomicStampedReference解决方案

#### BlockingQueueDeepDive.java
- **实现类对比** - Array/Linked/Priority/Synchronous
- **阻塞vs非阻塞操作** - put/take vs offer/poll
- **生产者-消费者模式** - 完整示例
- **超时操作** - 带超时的offer/poll
- **批量操作** - drainTo的使用
- **公平性选择** - 各种实现的特性

#### ConcurrentHashMapDeepDive.java
- **原子操作方法** - putIfAbsent, compute, merge
- **并发性能** - 高并发场景测试
- **遍历操作** - forEach, 并行遍历
- **搜索操作** - search方法
- **reduce操作** - 聚合计算
- **容量管理** - 初始容量和并发级别
- **集合视图** - keySet等线程安全视图

#### LockDeepDive.java
- **ReentrantLock基础** - 可重入特性
- **公平vs非公平锁** - 性能对比
- **tryLock用法** - 非阻塞和超时获取
- **lockInterruptibly** - 可中断锁
- **Condition** - 条件变量使用
- **ReadWriteLock** - 读写分离
- **StampedLock** - 乐观读优化
- **死锁预防** - 最佳实践

## 运行方式

1. 使用Maven编译：
```bash
mvn compile
```

2. 运行示例：
```bash
# 编译并运行
mvn exec:java -Dexec.mainClass="com.example.juc.AtomicExample"

# 或者分别运行不同的示例
mvn exec:java -Dexec.mainClass="com.example.juc.ThreadPoolExample"
mvn exec:java -Dexec.mainClass="com.example.juc.SynchronizationExample"
mvn exec:java -Dexec.mainClass="com.example.juc.ConcurrentCollectionsExample"
```

#### StampedLockExample.java
- **乐观读模式** - 无阻塞读操作
- **悲观读锁** - 共享读锁
- **写锁** - 排他写锁
- **锁转换** - 读写锁之间转换
- **性能对比** - vs ReentrantReadWriteLock
- **注意事项** - 不可重入、无Condition

#### VarHandleExample.java
- **VarHandle基础** - 替代Unsafe的标准化方式
- **原子操作** - get/set, CAS操作
- **数组访问** - 原子访问数组元素
- **静态字段** - 原子访问静态字段
- **内存屏障** - fullFence, acquireFence, releaseFence
- **性能对比** - vs AtomicInteger

#### ConcurrencyOptimization.java
- **锁粒度优化** - 粗粒度vs细粒度锁
- **读写分离** - ReadWriteLock应用
- **LongAdder** - 高并发计数优化
- **批量操作** - drainTo批量处理
- **并行流** - parallelStream优化
- **锁分段** - Striping技术
- **对象复用** - 减少GC压力
- **减少上下文切换** - 合理设置线程池
- **异步处理** - IO密集型任务优化

#### VirtualThreadExample.java
- **虚拟线程创建** - Thread.ofVirtual()
- **虚拟vs平台线程** - 性能和资源对比
- **阻塞操作** - 虚拟线程中的IO处理
- **ExecutorService** - newVirtualThreadPerTaskExecutor
- **结构化并发** - StructuredTaskScope
- **同步机制** - synchronized在虚拟线程中
- **注意事项** - 不适用场景和限制

## 学习路线

详细的学习路线请参考 [LEARNING_ROADMAP.md](LEARNING_ROADMAP.md)，包含：
- 8个学习阶段（从基础到高级）
- 每个主题的学习时间和练习建议
- 学习检查清单
- 推荐书籍和资源

## 学习目标

- 理解Java并发编程的核心概念
- 掌握各种同步机制的使用场景
- 学会选择合适的数据结构和工具类
- 能够分析和解决并发编程中的常见问题
- 掌握并发性能优化技巧

#### CompletableFutureExample.java
- **异步任务创建** - supplyAsync, runAsync
- **链式调用** - thenApply, thenAccept, thenRun
- **组合多个Future** - thenCombine, allOf, anyOf
- **异常处理** - exceptionally, handle, whenComplete
- **实际场景** - 并行获取用户数据汇总

#### CompletableFutureAdvanced.java
- **多Future组合** - 复杂的并行场景
- **异常处理进阶** - 异常链和恢复
- **超时控制** - orTimeout, completeOnTimeout
- **批量处理** - 并发度控制
- **条件执行** - 基于结果的分支
- **重试模式** - 失败自动重试
- **背压控制** - 控制提交速率
- **实际场景** - 电商订单处理

#### ThreadPoolBestPractices.java
- **避免Executors** - 为什么不推荐
- **正确创建** - ThreadPoolExecutor参数设置
- **线程池大小** - CPU密集型/IO密集型
- **自定义ThreadFactory** - 命名和配置线程
- **自定义拒绝策略** - 任务溢出处理
- **优雅关闭** - shutdown正确流程
- **线程池监控** - 状态查询和告警
- **常见陷阱** - 5个典型坑

#### ForkJoinPoolDeepDive.java
- **工作窃取算法** - ForkJoinPool原理
- **大任务分解** - RecursiveTask示例
- **并行流使用** - parallelStream vs ForkJoin
- **任务类型** - RecursiveTask vs RecursiveAction
- **对比分析** - ForkJoinPool vs ThreadPool
- **最佳实践** - 阈值选择和资源管理

#### ConcurrencyPatterns.java
- **生产者-消费者** - BlockingQueue实现
- **Guarded Suspension** - 条件等待模式
- **Balking模式** - 条件不满足直接返回
- **Promise模式** - Future的实现原理
- **Thread-Per-Message** - 每条消息一个线程
- **Worker Thread** - 工作线程池模式
- **Two-Phase Termination** - 两阶段终止模式

#### VolatileExample.java
- **可见性演示** - 线程间变量的可见性
- **非原子性** - volatile不能保证复合操作原子性
- **指令重排序** - volatile禁止指令重排序
- **volatile vs synchronized** - 对比分析
- **单例模式** - double-check锁中的volatile作用

#### SynchronizedDeepDive.java
- **同步方法 vs 代码块** - 使用场景对比
- **对象锁 vs 类锁** - 锁的粒度
- **可重入性** - 同一线程可重复获取锁
- **锁升级机制** - 偏向锁→轻量级锁→重量级锁
- **锁释放时机** - 自动释放的条件
- **死锁检测** - 死锁场景及解决方案
- **synchronized vs ReentrantLock** - 详细对比

#### DelayQueueExample.java
- **基本使用** - 实现Delayed接口
- **任务调度** - 延迟任务执行
- **缓存过期** - 自动清理过期缓存
- **动态任务** - 运行时添加延迟任务

#### TransferQueueExample.java
- **transfer()** - 等待消费者接收
- **tryTransfer()** - 非阻塞传输
- **tryTransfer(timeout)** - 带超时的传输
- **手递手模式** - 实时数据传输场景
- **vs BlockingQueue** - 使用场景对比

#### ConcurrentSkipListMapExample.java
- **有序存储** - 基于跳表的有序Map
- **导航方法** - floor, ceiling, lower, higher
- **范围查询** - subMap, headMap, tailMap
- **并发写入** - 高并发下的有序性
- **vs ConcurrentHashMap** - 场景选择
- **ConcurrentSkipListSet** - 有序Set

#### ThreadLocalExample.java
- **基本使用** - set, get, remove
- **线程隔离** - 每个线程独立副本
- **用户上下文** - 在多个方法间传递上下文
- **InheritableThreadLocal** - 子线程继承父线程值
- **内存泄漏** - 正确清理ThreadLocal

#### FutureExample.java
- **Future基本使用** - submit, get
- **FutureTask** - 可手动执行的任务
- **超时控制** - get(timeout)
- **取消任务** - cancel, isCancelled
- **批量任务** - invokeAll, invokeAny

#### ExchangerExample.java
- **基本使用** - 两个线程交换数据
- **数据缓冲** - 生产者消费者缓冲区交换
- **超时交换** - 带超时的exchange
- **vs 其他同步器** - 对比分析

#### PhaserExample.java
- **基本使用** - 多阶段同步
- **动态注册** - 运行时增减参与者
- **vs CyclicBarrier** - 使用场景对比
- **实际场景** - 多阶段并行处理

#### PriorityBlockingQueueExample.java
- **基本使用** - 按优先级取出
- **自定义优先级** - 实现Comparable
- **实际场景** - 多消费者处理优先级任务
- **对比分析** - vs 其他阻塞队列

#### SynchronousQueueExample.java
- **基本使用** - 直接交接
- **任务传递** - 无缓冲传递给工作线程
- **公平 vs 非公平** - 两种模式对比
- **对比分析** - vs 其他队列

#### CopyOnWriteArraySetExample.java
- **基本使用** - 线程安全Set
- **并发读写** - 读多写少场景
- **vs HashSet** - 对比分析
- **适用场景** - 事件监听器、白名单

每个示例都包含了详细的注释和实际的使用场景，帮助你深入理解JUC的各种特性。