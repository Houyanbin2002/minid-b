package com.dbgpt.java.core.awel.operator;

import com.dbgpt.java.core.awel.dag.DAGContext;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 对应 DB-GPT Python 源码的 MapOperator
 * 用于对输入数据进行转换的 AWEL 算子
 */
public class MapOperator<I, O> extends BaseOperator<I, O> {

    private final Function<I, O> mapFunction;

    public MapOperator(String operatorId, String operatorName, Function<I, O> mapFunction) {
        super(operatorId, operatorName);
        this.mapFunction = mapFunction;
    }

    @Override
    public CompletableFuture<O> call(I input, DAGContext context) {
        return CompletableFuture.supplyAsync(() -> {
            O result = mapFunction.apply(input);
            // 记录到 DAG 局部上下文中
            context.setNodeOutput(this.operatorId, result);
            return result;
        });
    }
}
