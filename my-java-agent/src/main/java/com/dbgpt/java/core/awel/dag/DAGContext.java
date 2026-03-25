package com.dbgpt.java.core.awel.dag;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对应 DB-GPT Python 源码的 DAGContext
 * 每个 AWEL Pipeline 运行时的全局级上下文对象
 */
public class DAGContext {
    private final String runId;
    private final Map<String, Object> nodeOutputs = new ConcurrentHashMap<>();

    public DAGContext() {
        this.runId = UUID.randomUUID().toString();
    }

    public void setNodeOutput(String nodeId, Object output) {
        this.nodeOutputs.put(nodeId, output);
    }

    public Object getNodeOutput(String nodeId) {
        return this.nodeOutputs.get(nodeId);
    }

    public String getRunId() {
        return runId;
    }
}
