package com.dbgpt.java.core.rag.retriever;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import java.util.List;

/**
 * 等价于 DB-GPT 的 python/rag/retriever/base.py -> BaseRetriever
 * 复刻 RAG 检索器的标准抽象形式（支持纯向量、BM25 混合搜索等拓展）
 */
public abstract class BaseRetriever {

    protected final VectorStore vectorStore;
    protected int topK = 4;
    protected double scoreThreshold = 0.5;

    public BaseRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 等价于 _retrieve() 方法，执行核心查询召回并附带元数据
     */
    public abstract List<Document> retrieve(String query);

    public BaseRetriever withTopK(int topK) {
        this.topK = topK;
        return this;
    }
}