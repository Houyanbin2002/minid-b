package com.dbgpt.java.core.awel.operator;

import com.dbgpt.java.core.awel.dag.DAGContext;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * 对应 DB-GPT Python 源码的 JoinOperator
 * 等待多个上游节点完成后，合并它们的输出
 */
public class JoinOperator<I1, I2, O> {

    private final String operatorId;
    private final BiFunction<I1, I2, O> joinFunction;

    public JoinOperator(String operatorId, BiFunction<I1, I2, O> joinFunction) {
        this.operatorId = operatorId;
        this.joinFunction = joinFunction;
    }

    public CompletableFuture<O> call(CompletableFuture<I1> future1, CompletableFuture<I2> future2, DAGContext context) {
        return future1.thenCombineAsync(future2, (result1, result2) -> {
            O joinedResult = joinFunction.apply(result1, result2);
            context.setNodeOutput(this.operatorId, joinedResult);
            return joinedResult;
        });
    }
}
