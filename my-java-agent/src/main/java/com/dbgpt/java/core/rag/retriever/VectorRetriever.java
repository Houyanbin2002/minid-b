package com.dbgpt.java.core.rag.retriever;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import java.util.List;

/**
 * 等价于 DB-GPT 的 EmbeddingRetriever
 * 依赖具体的 Embedding 模型在 VectorDB 中进行余弦相似度召回
 */
public class VectorRetriever extends BaseRetriever {

    public VectorRetriever(VectorStore vectorStore) {
        super(vectorStore);
    }

    @Override
    public List<Document> retrieve(String query) {
        SearchRequest request = SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(scoreThreshold);
        
        return vectorStore.similaritySearch(request);
    }
}