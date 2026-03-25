package com.dbgpt.java.awel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 抽象的 AWEL 流式计算节点
 * 模拟 DB-GPT 的 DAG Node，支持异步流式处理
 * @param <I> 输入类型
 * @param <O> 输出类型
 */
public abstract class AwelNode<I, O> implements Function<I, CompletableFuture<O>> {

    public abstract CompletableFuture<O> process(I input);

    @Override
    public CompletableFuture<O> apply(I input) {
        return process(input);
    }
    
    /**
     * 连接下一个节点 (DAG 中的边)
     */
    public <R> AwelNode<I, R> next(AwelNode<O, R> nextNode) {
        AwelNode<I, O> self = this;
        return new AwelNode<I, R>() {
            @Override
            public CompletableFuture<R> process(I input) {
                return self.process(input).thenCompose(nextNode::apply);
            }
        };
    }
}
