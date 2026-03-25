package com.dbgpt.java.core.awel.operator;

import com.dbgpt.java.core.awel.dag.DAGContext;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * 对应 DB-GPT Python 源码的 BranchOperator
 * 用于分支路由的 AWEL 算子，根据条件将流导向不同的下游
 */
public class BranchOperator<I> extends BaseOperator<I, Boolean> {

    private final Predicate<I> condition;

    public BranchOperator(String operatorId, String operatorName, Predicate<I> condition) {
        super(operatorId, operatorName);
        this.condition = condition;
    }

    @Override
    public CompletableFuture<Boolean> call(I input, DAGContext context) {
        return CompletableFuture.supplyAsync(() -> {
            boolean result = condition.test(input);
            context.setNodeOutput(this.operatorId, result);
            return result;
        });
    }
}
