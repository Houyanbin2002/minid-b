package com.dbgpt.java.core.memory.manager;

import com.dbgpt.java.core.agent.core.AgentMessage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

/**
 * 等价于 DB-GPT 的 ChatHistoryMemory / ContextManager
 * 用来管理【多轮会话】（根据 SessionID 隔离）
 * 这是实现“连贯聊天”与 L2 记忆的基础
 */
public class ChatSessionMemoryManager {

    // K=SessionId (如用户ID/会话ID) V=该用户的历史消息
    private final Map<String, List<AgentMessage>> sessionStores = new ConcurrentHashMap<>();
    
    // 配置最大上下文截断窗口 (防止挤爆 Token)
    private static final int MAX_HISTORY_WINDOW = 20;

    /**
     * 将消息存入多轮会话
     */
    public void appendMessage(String sessionId, AgentMessage msg) {
        sessionStores.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(msg);
        
        // 简单滑窗截断：如果超过 MAX，则丢弃最老的 (实际 DB-GPT 中会压缩或者存入 VectorStore)
        List<AgentMessage> history = sessionStores.get(sessionId);
        if (history.size() > MAX_HISTORY_WINDOW) {
            history.remove(0); 
        }
    }

    /**
     * 加载当前用户的多轮聊天记录
     */
    public List<AgentMessage> getHistory(String sessionId) {
        return sessionStores.getOrDefault(sessionId, new ArrayList<>());
    }

    /**
     * 清空某一会话
     */
    public void clearSession(String sessionId) {
        sessionStores.remove(sessionId);
    }
    
    /**
     * 一键导出给 LLM 的格式化聊天记录
     */
    public String getFormattedHistory(String sessionId) {
        List<AgentMessage> history = getHistory(sessionId);
        StringBuilder sb = new StringBuilder();
        for (AgentMessage msg : history) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\\n");
        }
        return sb.toString();
    }
}
