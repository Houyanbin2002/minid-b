package com.dbgpt.java.core.agent.core;

import com.dbgpt.java.core.agent.profile.AgentProfile;
import com.dbgpt.java.core.memory.AgentMemory;
import java.util.concurrent.CompletableFuture;

/**
 * 对应 DB-GPT Python 源码的 ConversableAgent
 * 多智能体系统的核心通信基类，支持发送(send)、接收(receive)、生成回复(generateReply)
 */
public abstract class ConversableAgent {

    protected final AgentProfile profile;
    protected final AgentMemory memory;

    public ConversableAgent(AgentProfile profile, AgentMemory memory) {
        this.profile = profile;
        this.memory = memory;
    }

    /**
     * 发送消息给另一个 Agent
     */
    public CompletableFuture<Void> send(AgentMessage message, ConversableAgent recipient) {
        return recipient.receive(message, this);
    }

    /**
     * 接收来自另一个 Agent 的消息，并触发思考和回复
     */
    public CompletableFuture<AgentMessage> receive(AgentMessage message, ConversableAgent sender) {
        // 1. 将接收到的消息存入本地短时记忆 (L1)
        this.memory.appendMessage(message);

        // 2. 将结果处理或推回给发件者，并返回 Future 供外部编排
        return this.generateReply(message, sender).thenApply(replyMsg -> {
            if (replyMsg != null) {
                // 将我自己的回答存入记忆
                this.memory.appendMessage(replyMsg);
            }
            return replyMsg;
        });
    }

    /**
     * 根据接收到的消息，结合 LLM 生成回复内容 (DB-GPT中的 `_generate_oai_reply` 或 `generate_reply`)
     */
    protected abstract CompletableFuture<AgentMessage> generateReply(AgentMessage message, ConversableAgent sender);

    public String getName() {
        return profile.getName();
    }
}