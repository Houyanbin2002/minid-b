package com.dbgpt.java.core.awel.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 等价于 DB-GPT 里的 MQ 操作 (或者 PubSub)
 * 利用 Redis 实现 AWEL 节点间的异步消息队列发布/订阅
 */
@Service
public class NodeMessageQueue {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public NodeMessageQueue(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 发送节点消息到指定的 Queue
     */
    public void publishMessage(String topic, Object payload) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(payload);
            redisTemplate.convertAndSend(topic, jsonMessage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize message payload", e);
        }
    }
}
