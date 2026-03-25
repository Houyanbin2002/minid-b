package com.dbgpt.java.core.rag.retriever.rerank;

import com.dbgpt.java.core.rag.knowledge.Chunk;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 对应 DB-GPT Python 源码中的 LLMReranker / CoheRerank
 * 使用大模型（如通义千问等）去对粗筛结果进行精细化阅读和打分重排
 */
public class DefaultLLMReranker implements BaseReranker {

    private final ChatClient chatClient;

    public DefaultLLMReranker(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public List<Chunk> rerank(String query, List<Chunk> candidates, int topK) {
        System.out.println("[Reranker] Received " + candidates.size() + " candidates. Starting LLM precision ranking...");
        
        // 【注：在真实工业场景中，这里通常调用专门的 BGE-Reranker 小模型服务，
        //  或者用一次并发的大模型 Prompt 对每个段落进行打分。此处展示重排抽象拦截器的骨架】
        
        for (Chunk chunk : candidates) {
            // 模拟或者接入真实的大语言模型为各个 Chunk 与 Query 的一致度打分
            // chunk.setScore( ...LLM Call... ); 
            chunk.setScore(Math.random()); // 随机模拟打分
        }

        // 依据新算出的维度给资料重新排序，并截取最精准的前 topK 送给 Agent
        return candidates.stream()
                .sorted(Comparator.comparingDouble(Chunk::getScore).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }
}
