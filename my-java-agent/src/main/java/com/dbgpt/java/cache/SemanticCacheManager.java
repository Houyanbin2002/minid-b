package com.dbgpt.java.cache;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class SemanticCacheManager {

    @Autowired
    private StringRedisTemplate redisTemplate; // 用于 O(1) 的精确匹配

    @Autowired
    private VectorStore milvusStore;           // 用于相似度的向量匹配 (HNSW)

    private static final double SIMILARITY_THRESHOLD = 0.95; // 相似度阈值极高

    public String checkCache(String userQuery) {
        // 第一层：短路拦截 - Redis 精确匹配 (耗时 2ms 以内)
        String exactMatch = redisTemplate.opsForValue().get("cache:exact:" + userQuery);
        if (exactMatch != null) {
            System.out.println("[Cache] 命中 Redis 精确匹配");
            return exactMatch; // 瞬间返回
        }

        // 第二层：语义缓存 - Milvus 向量匹配 (耗时 30-50ms)
        List<Document> semanticMatches = milvusStore.similaritySearch(
                SearchRequest.query(userQuery).withTopK(1).withSimilarityThreshold(SIMILARITY_THRESHOLD)
        );
        
        if (!semanticMatches.isEmpty()) {
            String cachedAnswer = (String) semanticMatches.get(0).getMetadata().get("cached_answer");
            if (cachedAnswer != null) {
                System.out.println("[Cache] 命中 Milvus 语义由于相似度极高: " + userQuery);
                return cachedAnswer; // 命中语义缓存，免触发后续 Agent 网络
            }
        }
        
        return null; // 未命中
    }

    public void putCache(String query, String answer) {
        // 缓存写入 Redis 
        redisTemplate.opsForValue().set("cache:exact:" + query, answer, 24, TimeUnit.HOURS);
        
        // 缓存写入 Milvus
        Document cacheDoc = new Document(query, Map.of(
            "cached_answer", answer,
            "type", "semantic_cache"
        ));
        milvusStore.add(List.of(cacheDoc));
    }
}
