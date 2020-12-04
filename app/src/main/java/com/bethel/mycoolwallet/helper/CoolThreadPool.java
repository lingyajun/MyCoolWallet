package com.bethel.mycoolwallet.helper;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池
 *
 * 配置的参数归纳为：
 * 核心线程数 = CPU数+2
 * 最大线程数 = CPU数*3 + 5
 * 非核心线程的超时时间为2秒
 * 任务队列的容量为128
 */
public final class CoolThreadPool {
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();//CPU数
    private static final int CORE_POOL_SIZE = CPU_COUNT + 2;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 3 + 5;
    private static final int KEEP_ALIVE = 2;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "CoolThreadPool #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<>(128);

    private static final Executor THREAD_POOL_EXECUTOR
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);

    public static void execute(Runnable runnable) {
        THREAD_POOL_EXECUTOR.execute(runnable);
    }
}
