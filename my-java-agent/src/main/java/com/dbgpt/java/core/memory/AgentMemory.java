package com.dbgpt.java.core.memory;

import com.dbgpt.java.core.agent.core.AgentMessage;
import com.dbgpt.java.core.memory.manager.MemorySummarizer;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 对应 DB-GPT 的 Memory 基类（如 L1 短时对话记忆）
 */
public class AgentMemory {

    private final List<AgentMessage> messageHistory = new ArrayList<>();
    private final Map<String, Object> variables = new ConcurrentHashMap<>();
    
    // DB-GPT Memory 防溢出机制参数
    private static final int MAX_TOKEN_TRUNCATION_LIMIT = 10;
    private MemorySummarizer summarizer;
    private String currentSummary;

    // 默认构造
    public AgentMemory() {}
    
    // 带摘要器的构造 (防溢出)
    public AgentMemory(MemorySummarizer summarizer) {
        this.summarizer = summarizer;
    }

    public synchronized void appendMessage(AgentMessage message) {
        this.messageHistory.add(message);
        
        // 触发 4.3 记忆防溢出与摘要
        if (summarizer != null && this.messageHistory.size() > MAX_TOKEN_TRUNCATION_LIMIT) {
             System.out.println("[Memory System] 触及上下文阈值(>" + MAX_TOKEN_TRUNCATION_LIMIT + "条)，正在触发 Memory Summarization 机制...");
             
             // 拿出一半的旧消息进行浓缩
             List<AgentMessage> oldMessages = new ArrayList<>(this.messageHistory.subList(0, 5));
             AgentMessage summaryMsg = summarizer.summarize(oldMessages, this.currentSummary);
             
             this.currentSummary = summaryMsg.getContent();
             // 清理掉被压缩的旧消息并存入压缩成果
             this.messageHistory.subList(0, 5).clear();
             this.messageHistory.add(0, summaryMsg);
        }
    }

    public synchronized List<AgentMessage> getMessages() {
        return new ArrayList<>(this.messageHistory);
    }

    public Map<String, Object> getVariables() {
        return this.variables;
    }

    public synchronized void clear() {
        this.messageHistory.clear();
        this.variables.clear();
        this.currentSummary = null;
    }

