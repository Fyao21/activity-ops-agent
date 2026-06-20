# Java 并发复习资料

## 线程和进程

进程是操作系统资源分配的基本单位，线程是 CPU 调度的基本单位。一个进程可以包含多个线程，线程共享进程的堆、方法区、文件句柄等资源，但每个线程有自己的程序计数器、虚拟机栈和本地方法栈。多线程可以提升并发处理能力，但也会带来线程安全、上下文切换、锁竞争和调试复杂度。

Java 中创建线程的方式包括继承 Thread、实现 Runnable、实现 Callable 配合 FutureTask，以及使用线程池。实际业务开发中不建议频繁手动创建线程，而应使用线程池统一管理线程生命周期。线程创建和销毁有成本，线程数量过多还会带来上下文切换和内存占用。

判断是否需要多线程时，要看任务类型。如果是 CPU 密集型任务，线程数通常接近 CPU 核数；如果是 IO 密集型任务，线程数可以适当大于 CPU 核数，但仍需要结合接口耗时、连接池大小和下游承载能力。

## Java 内存模型

Java 内存模型定义了多线程环境下变量读写的可见性、有序性和原子性规则。每个线程都有自己的工作内存，线程对共享变量的操作不是直接操作主内存，而是可能先读到本地工作内存中。因此一个线程修改变量后，另一个线程不一定马上看到，这就是可见性问题。

`volatile` 可以保证变量的可见性和禁止指令重排序，但不能保证复合操作的原子性。例如 `count++` 包含读取、加一、写回三个步骤，即使 count 是 volatile，也可能在多线程下丢失更新。需要原子递增时可以使用 `AtomicInteger` 或加锁。

有序性问题来自编译器和 CPU 的指令重排序。只要单线程语义不变，编译器和 CPU 可能调整执行顺序。多线程场景下，如果没有同步约束，重排序可能导致意外结果。`volatile`、`synchronized`、Lock、线程启动和 join 等都会建立 happens-before 关系。

## synchronized

`synchronized` 是 Java 内置锁，可以修饰实例方法、静态方法或代码块。修饰实例方法时锁对象是当前实例；修饰静态方法时锁对象是 Class 对象；修饰代码块时可以指定锁对象。进入 synchronized 代码块前需要获得监视器锁，退出时释放锁。

`synchronized` 同时保证互斥、可见性和有序性。线程释放锁前会把共享变量刷新到主内存，线程获得锁后会重新读取共享变量。因此它不仅解决多个线程同时修改的问题，也解决可见性问题。

现代 JVM 对 synchronized 做了大量优化，例如偏向锁、轻量级锁、自旋锁和锁消除。虽然在很多场景下性能已经不错，但锁粒度仍然需要控制。锁范围过大会降低并发；锁对象选择不当可能导致不同业务互相阻塞；锁顺序不一致可能造成死锁。

## ReentrantLock

ReentrantLock 是 JUC 包提供的可重入锁。相比 synchronized，它支持尝试加锁、可中断加锁、公平锁和多个 Condition 条件队列。可重入表示同一个线程获得锁后，可以再次获得同一把锁，不会被自己阻塞，但释放时也要释放相同次数。

ReentrantLock 使用时必须在 finally 中释放锁，否则异常会导致锁无法释放，其他线程永久阻塞。典型代码是先 `lock.lock()`，然后在 try 中执行业务，finally 中调用 `lock.unlock()`。

公平锁会按请求顺序分配锁，减少线程饥饿，但吞吐量通常低于非公平锁。大多数业务场景使用默认非公平锁即可。Condition 可以实现更精细的等待通知机制，例如生产者消费者模型中的 notEmpty 和 notFull。

## 线程池

线程池用于复用线程、控制并发数量、管理任务队列和统一处理拒绝策略。Java 常用 `ThreadPoolExecutor`，核心参数包括 corePoolSize、maximumPoolSize、keepAliveTime、workQueue、threadFactory 和 rejectedExecutionHandler。

任务提交后，如果运行线程数小于 corePoolSize，会创建核心线程；如果核心线程已满，任务进入队列；如果队列满且线程数小于 maximumPoolSize，会创建非核心线程；如果仍无法处理，就执行拒绝策略。常见拒绝策略包括抛异常、调用者执行、丢弃当前任务和丢弃最旧任务。

不建议直接使用 `Executors.newFixedThreadPool`、`newCachedThreadPool` 等快捷方法，因为它们可能使用无界队列或无限线程数，导致 OOM。实际项目应根据业务类型配置线程数、队列大小、线程名前缀和拒绝策略。线程名非常重要，能帮助排查日志和线程 dump。

## ThreadLocal

ThreadLocal 用于为每个线程保存一份独立变量副本，常用于保存用户上下文、TraceId、日期格式化器等。它的底层是每个线程对象中维护 ThreadLocalMap，key 是 ThreadLocal 的弱引用，value 是实际数据。

ThreadLocal 最大风险是内存泄漏。在线程池中，线程会被复用，如果任务结束后没有 remove，旧 value 可能一直挂在线程上，影响后续任务并占用内存。因此使用 ThreadLocal 后应在 finally 中调用 remove。

在 Web 项目中，如果用 ThreadLocal 保存登录用户信息，请求结束后必须清理。否则不同请求复用同一工作线程时，可能出现用户信息串号，造成严重安全问题。

## CAS 和原子类

CAS 是 Compare And Swap 的缩写，表示比较并交换。它会比较内存中的值是否等于期望值，如果相等就更新为新值，否则失败。CAS 是很多无锁并发工具的基础，例如 AtomicInteger、AtomicLong、AtomicReference。

CAS 的优点是避免阻塞，适合竞争不激烈、操作简单的场景。缺点包括 ABA 问题、自旋开销和只能保证单个变量的原子更新。ABA 问题是指变量从 A 变成 B 又变回 A，CAS 只看到值仍然是 A，却不知道中间变化过。可以使用版本号或 AtomicStampedReference 解决。

高并发计数场景可以使用 LongAdder。它通过分段累加降低热点竞争，吞吐通常高于 AtomicLong，但读取时需要汇总多个单元，因此更适合统计计数而不是强一致数值判断。

## 常见面试问题

1. volatile 能保证原子性吗？
2. synchronized 和 ReentrantLock 有什么区别？
3. 线程池核心参数有哪些？
4. 为什么不建议使用 Executors 快捷方法？
5. ThreadLocal 为什么可能内存泄漏？
6. CAS 有什么问题？
7. CPU 密集型和 IO 密集型任务线程数怎么估算？
