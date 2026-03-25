package com.dbgpt.java.core.memory.manager;

import com.dbgpt.java.core.agent.core.AgentMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 等价于 DB-GPT 中真正的基于外存的短时/持久记忆管理
 * 这里使用 Redis 替代最初在内存里的 ConcurrentHashMap
 */
@Service
public class RedisMemoryManager {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String MEMORY_PREFIX = "dbgpt:memory:session:";
    private static final int MAX_HISTORY_WINDOW = 20;

    public RedisMemoryManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 追加新消息到 Redis List (利用 LTRIM 实现滑窗)
     */
    public void appendMessage(String sessionId, AgentMessage msg) {
        String key = MEMORY_PREFIX + sessionId;
        try {
            String val = objectMapper.writeValueAsString(msg);
            redisTemplate.opsForList().rightPush(key, val);
            // 滑窗截断：只保留最新的 20 条，抛弃更老的
            redisTemplate.opsForList().trim(key, -MAX_HISTORY_WINDOW, -1);
            // 设置过期时间，比如 7 天
            redisTemplate.expire(key, Duration.ofDays(7));
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize message: " + e.getMessage());
        }
    }

    /**
     * 提取历史记录
     */
    public List<AgentMessage> getHistory(String sessionId) {
        String key = MEMORY_PREFIX + sessionId;
        List<String> rawMessages = redisTemplate.opsForList().range(key, 0, -1);
        
        if (rawMessages == null || rawMessages.isEmpty()) {
            return new ArrayList<>();
        }

        return rawMessages.stream().map(raw -> {
            try {
                return objectMapper.readValue(raw, AgentMessage.class);
            } catch (JsonProcessingException e) {
                return null;
            }
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());
    }

    public void clearSession(String sessionId) {
        redisTemplate.delete(MEMORY_PREFIX + sessionId);
    }
}
