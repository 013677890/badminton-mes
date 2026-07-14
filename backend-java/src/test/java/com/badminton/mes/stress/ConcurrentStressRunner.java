package com.badminton.mes.stress;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/** 固定工作线程、同步起跑的轻量并发压力执行器。 */
final class ConcurrentStressRunner {

    private static final int WORKERS = Math.max(4,
            Math.min(16, Runtime.getRuntime().availableProcessors() * 2));

    static Duration run(String module, int operations, IntConsumer operation) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(WORKERS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(WORKERS);
        AtomicInteger cursor = new AtomicInteger();
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();

        for (int worker = 0; worker < WORKERS; worker++) {
            executor.execute(() -> {
                try {
                    start.await();
                    int index;
                    while ((index = cursor.getAndIncrement()) < operations) {
                        try {
                            operation.accept(index);
                        } catch (Throwable failure) {
                            failures.add(failure);
                        }
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    failures.add(interrupted);
                } finally {
                    done.countDown();
                }
            });
        }

        long startedAt = System.nanoTime();
        start.countDown();
        boolean completed = done.await(60, TimeUnit.SECONDS);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);
        executor.shutdownNow();

        assertThat(completed).as(module + " stress run timed out").isTrue();
        assertThat(failures).as(module + " concurrent failures").isEmpty();
        assertThat(cursor.get()).isGreaterThanOrEqualTo(operations);
        return elapsed;
    }

    private ConcurrentStressRunner() {
    }
}
