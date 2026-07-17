package com.badminton.mes.stress;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicIntegerArray;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 并发压力执行器本身的调度、计数和失败传播测试。 @author 范家权 */
class ConcurrentStressRunnerTest {

    @Test
    void everyOperationIndexIsExecutedExactlyOnce() throws Exception {
        int operations = 2_000;
        AtomicIntegerArray hits = new AtomicIntegerArray(operations);

        ConcurrentStressRunner.run("runner-count", operations,
                index -> hits.incrementAndGet(index));

        for (int index = 0; index < operations; index++) {
            assertThat(hits.get(index)).as("operation %s", index).isEqualTo(1);
        }
    }

    @Test
    void workerFailureIsPropagatedToTheCallingTest() {
        assertThatThrownBy(() -> ConcurrentStressRunner.run(
                "runner-failure", 100, index -> {
                    if (index == 37) {
                        throw new IllegalStateException("expected worker failure");
                    }
                }))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("concurrent failures");
    }
}
