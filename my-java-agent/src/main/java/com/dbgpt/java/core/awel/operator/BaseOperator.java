package com.dbgpt.java.core.awel.operator;

import com.dbgpt.java.core.awel.dag.DAGContext;
import java.util.concurrent.CompletableFuture;

/**
 * 等价于 DB-GPT 的 python/awel/operator/base.py -> BaseOperator
 * 是所有工作流处理节点的绝对基类
 */
public abstract class BaseOperator<I, O> {

    protected String operatorId;
    protected String operatorName;

    public BaseOperator(String operatorId, String operatorName) {
        this.operatorId = operatorId;
        this.operatorName = operatorName;
    }

    /**
     * DB-GPT AWEL 中的核心编排逻辑，带上下文 context
     * @param input 源数据
     * @param context AWEL 的执行大上下文，记录上游节点的执行产物
     */
    public abstract CompletableFuture<O> call(I input, DAGContext context);

    public String getOperatorId() {
        return operatorId;
    }
}