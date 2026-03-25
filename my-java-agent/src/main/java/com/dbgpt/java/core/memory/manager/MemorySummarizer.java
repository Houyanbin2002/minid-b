package com.dbgpt.java.core.memory.manager;

import com.dbgpt.java.core.agent.core.AgentMessage;
import org.springframework.ai.chat.client.ChatClient;
import java.util.List;

/**
 * 对应 DB-GPT 的历史上下文摘要机制 (Memory Summarization)
 * 当 Token 堆积或者多 Agent 报告超过阈值时，自动提取最早的历史进行摘要缩写。
 */
public class MemorySummarizer {

    private final ChatClient chatClient;

    public MemorySummarizer(ChatClient.Builder builder) {
        // 创建一个专注于总结的无闲聊模型客户端
        this.chatClient = builder
                .defaultSystem("你是系统底层的记忆压缩器。请将传入的对话历史总结为高度凝练的背景信息，保留所有实体参数（如身高、目标、结果数据等），去除多余的寒暄。")
                .build();
    }

    public AgentMessage summarize(List<AgentMessage> oldMessages, String previousSummary) {
        StringBuilder sb = new StringBuilder();
        if (previousSummary != null && !previousSummary.isEmpty()) {
            sb.append("这是之前的总结：").append(previousSummary).append("\n\n");
        }
        sb.append("这是需要被压缩的历史记录：\n");
        for (AgentMessage msg : oldMessages) {
            sb.append(msg.getName() != null ? msg.getName() : msg.getRole())
              .append(": ").append(msg.getContent()).append("\n");
        }

        String summaryResult = chatClient.prompt()
                .user(sb.toString())
                .call()
                .content();

        return AgentMessage.builder()
                .role("system")
                .name("MemorySummary")
                .content("【系统防溢出历史摘要】: \n" + summaryResult)
                .build();
    }
}