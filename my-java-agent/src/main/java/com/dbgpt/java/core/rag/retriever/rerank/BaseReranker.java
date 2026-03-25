package com.dbgpt.java.core.rag.retriever.rerank;

import com.dbgpt.java.core.rag.knowledge.Chunk;
import java.util.List;

/**
 * 对应 DB-GPT Python 中的 BaseReranker
 * 对两路检索 (比如混合了 BM25 关键词和 Vector 向量提取) 的结果进行深度大模型重排/打分重排
 */
public interface BaseReranker {
    
    /**
     * @param query 用户的原始提问
     * @param candidates 初筛回来的几百个候选文档切片
     * @param topK 最终只要排名前 K 个最精准的
     * @return 重新按精准度排序并裁剪后的切片列表
     */
    List<Chunk> rerank(String query, List<Chunk> candidates, int topK);
}
