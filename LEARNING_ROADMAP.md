# JUC 学习路线

本路线图提供从入门到精通的 Java 并发编程学习路径。

## 第一阶段：基础概念（1-2周）

### 学习目标
- 理解并发与并行的区别
- 掌握线程基础概念和生命周期
- 理解线程安全问题

### 学习内容
| 主题 | 文件 | 预计时间 |
|------|------|----------|
| 线程基础 | `ThreadBasicExample.java` | 2天 |
| 线程状态与生命周期 | `ThreadBasicExample.java` | 1天 |
| 线程中断与协作 | `ThreadBasicExample.java` | 1天 |
| Volatile 可见性 | `VolatileExample.java` | 1天 |
| Synchronized 同步 | `SynchronizedDeepDive.java` | 2天 |

### 练习建议
- 使用 `Thread.join()` 实现任务顺序执行
- 使用 `volatile` 实现状态标记模式
- 使用 `synchronized` 实现线程安全计数器

---

## 第二阶段：锁机制（2-3周）

### 学习目标
- 理解 Java 锁机制原理
- 掌握各种锁的使用场景
- 学会选择合适的锁类型

### 学习内容
| 主题 | 文件 | 预计时间 |
|------|------|----------|
| ReentrantLock 基础 | `LockDeepDive.java` | 2天 |
| ReadWriteLock 读写锁 | `LockDeepDive.java` | 2天 |
| StampedLock 乐观读 | `StampedLockExample.java` | 2天 |
| Condition 条件变量 | `LockDeepDive.java` | 1天 |
| 锁性能对比 | `LockDeepDive.java` | 1天 |

### 练习建议
- 实现一个可重入的线程安全队列
- 使用 ReadWriteLock 优化读多写少场景
- 使用 StampedLock 实现缓存系统

---

## 第三阶段：原子类与 CAS（1-2周）

### 学习目标
- 理解 CAS 原理
- 掌握 Atomic 类使用
- 理解 ABA 问题及解决方案

### 学习内容
| 主题 | 文件 | 预计时间 |
|------|------|----------|
| Atomic 基础 | `AtomicExample.java` | 1天 |
| CAS 原理 | `AtomicDeepDive.java` | 1天 |
| LongAdder 高性能累加 | `AtomicDeepDive.java` | 1天 |
| ABA 问题解决 | `AtomicDeepDive.java` | 1天 |
| VarHandle 原子操作 | `VarHandleExample.java` | 1天 |

### 练习建议
- 使用 AtomicReference 实现无锁栈
- 使用 LongAdder 统计请求次数
- 使用 AtomicStampedReference 解决 ABA 问题

---

## 第四阶段：同步工具类（1-2周）

### 学习目标
- 掌握 JUC 同步工具类
- 理解各工具类的使用场景
- 能够设计协调多个线程的协作方案

### 学习内容
| 主题 | 文件 | 预计时间 |
|------|------|----------|
| CountDownLatch 倒计时 | `SynchronizationExample.java` | 1天 |
| CyclicBarrier 循环栅栏 | `SynchronizationExample.java` | 1天 |
| Semaphore 信号量 | `SynchronizationExample.java` | 1天 |
| Exchanger 数据交换 | `ExchangerExample.java` | 1天 |
| Phaser 多阶段同步 | `PhaserExample.java` | 1天 |

### 练习建议
- 使用 CountDownLatch 实现并发任务聚合
- 使用 CyclicBarrier 实现多阶段数据处理
- 使用 Semaphore 限流接口访问

---

## 第五阶段：并发集合（1-2周）

### 学习目标
- 掌握并发集合的使用
- 理解不同并发集合的适用场景
- 了解并发集合的内部实现原理

### 学习内容
| 主题 | 文件 | 预计时间 |
|------|------|----------|
| 并发集合基础 | `ConcurrentCollectionsExample.java` | 1天 |
| ConcurrentHashMap 深度 | `ConcurrentHashMapDeepDive.java` | 2天 |
| BlockingQueue 深度 | `BlockingQueueDeepDive.java` | 2天 |
| 有序并发集合 | `ConcurrentSkipListMapExample.java` | 1天 |
| 写时复制集合 | `CopyOnWriteArraySetExample.java` | 1天 |

### 练习建议
- 使用 ConcurrentHashMap 实现分布式缓存
- 使用 BlockingQueue 实现生产者-消费者模式
- 使用 ConcurrentSkipListMap 实现排行榜

---

## 第六阶段：线程池（2-3周）

### 学习目标
- 掌握线程池的使用和配置
- 理解线程池工作原理
- 能够进行线程池调优

### 学习内容
| 主题 | 文件 | 预计时间 |
|------|------|----------|
| 线程池基础 | `ThreadPoolExample.java` | 1天 |
| ForkJoinPool 分治 | `ForkJoinPoolDeepDive.java` | 2天 |
| 线程池最佳实践 | `ThreadPoolBestPractices.java` | 2天 |
| 线程池调优 | `ThreadPoolBestPractices.java` | 1天 |

### 练习建议
- 实现自定义线程工厂和拒绝策略
- 使用 ForkJoinPool 实现并行计算
- 监控和调优生产环境线程池

---

## 第七阶段：异步编程（2-3周）

### 学习目标
- 掌握 CompletableFuture 使用
- 理解异步编程模式
- 能够设计高性能异步系统

### 学习内容
| 主题 | 文件 | 预计时间 |
|------|------|----------|
| Future 基础 | `FutureExample.java` | 1天 |
| CompletableFuture 基础 | `CompletableFutureExample.java` | 2天 |
| CompletableFuture 进阶 | `CompletableFutureAdvanced.java` | 2天 |
| 虚拟线程 (Java 21+) | `VirtualThreadExample.java` | 2天 |

### 练习建议
- 使用 CompletableFuture 实现并行请求聚合
- 实现带重试和超时的异步任务
- 使用虚拟线程改造现有 IO 密集型应用

---

## 第八阶段：并发模式与优化（1-2周）

### 学习目标
- 掌握常见并发设计模式
- 理解并发性能优化方法
- 能够诊断和解决并发问题

### 学习内容
| 主题 | 文件 | 预计时间 |
|------|------|----------|
| 并发设计模式 | `ConcurrencyPatterns.java` | 2天 |
| 并发性能优化 | `ConcurrencyOptimization.java` | 2天 |
| ThreadLocal 线程隔离 | `ThreadLocalExample.java` | 1天 |

### 练习建议
- 实现一个高性能的线程池
- 分析并优化现有并发代码
- 实现 ThreadLocal 上下文传递

---

## 进阶专题

### 错误处理与调试
- 死锁诊断与预防
- 内存泄漏检测
- 并发异常传播

### 源码分析
- ReentrantLock 源码
- ConcurrentHashMap 源码
- ThreadPoolExecutor 源码

### 实战项目
- 高性能 RPC 框架
- 分布式限流组件
- 并发缓存系统

---

## 学习资源

### 推荐书籍
1. 《Java并发编程实战》- Brian Goetz
2. 《Java并发编程的艺术》- 方腾飞
3. 《深入理解Java虚拟机》- 周志明

### 推荐文章
- [Java并发指南](https://www.jianshu.com/nb/32940196)
- [阿里Java开发手册-并发处理](https://github.com/alibaba/p3c)
- [JDK Concurrency Tutorial](https://docs.oracle.com/javase/tutorial/essential/concurrency/)

---

## 学习检查清单

### 基础
- [ ] 能创建和管理线程
- [ ] 理解线程状态转换
- [ ] 能正确处理线程中断
- [ ] 理解 volatile 的作用

### 锁
- [ ] 能使用 synchronized 和 ReentrantLock
- [ ] 能选择合适的锁类型
- [ ] 理解死锁及其预防
- [ ] 能实现 Condition 条件等待

### 原子类
- [ ] 能使用 Atomic 类
- [ ] 理解 CAS 原理
- [ ] 知道 ABA 问题及解决方案
- [ ] 能使用 LongAdder

### 工具类
- [ ] 能使用 CountDownLatch
- [ ] 能使用 CyclicBarrier
- [ ] 能使用 Semaphore
- [ ] 能使用 Exchanger 和 Phaser

### 集合
- [ ] 能选择合适的并发集合
- [ ] 理解 ConcurrentHashMap 原理
- [ ] 能使用 BlockingQueue
- [ ] 知道何时使用 CopyOnWrite

### 线程池
- [ ] 能正确创建和配置线程池
- [ ] 理解线程池参数含义
- [ ] 能实现自定义线程池
- [ ] 能进行线程池调优

### 异步
- [ ] 能使用 CompletableFuture
- [ ] 能处理异步异常
- [ ] 能设计异步工作流
- [ ] 了解虚拟线程特性

### 优化
- [ ] 能分析并发性能瓶颈
- [ ] 能使用并发设计模式
- [ ] 能诊断死锁和内存泄漏
- [ ] 理解 ThreadLocal 原理
