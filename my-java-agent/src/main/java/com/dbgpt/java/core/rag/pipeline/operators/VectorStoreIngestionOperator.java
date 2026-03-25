package com.dbgpt.java.core.rag.pipeline.operators;

import com.dbgpt.java.core.awel.dag.DAGContext;
import com.dbgpt.java.core.awel.operator.BaseOperator;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 对应 DB-GPT 的 VectorStoreOperator
 * 职责：调用本地/远端的 Embedding 模型把区块转为向量数组，并直接 Save 到 Milvus/Redis
 */
public class VectorStoreIngestionOperator extends BaseOperator<List<Document>, Boolean> {

    private final VectorStore vectorStore;

    public VectorStoreIngestionOperator(String operatorId, VectorStore vectorStore) {
        super(operatorId, "VectorDatabaseIngestion");
        this.vectorStore = vectorStore;
    }

    @Override
    public CompletableFuture<Boolean> call(List<Document> chunkedDocuments, DAGContext context) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("[RAG Pipeline] Node 3 - Triggering Embedding Model and Saving to Milvus/Redis Store...");
            
            try {
                // Spring AI 原生的一键 Embedding 和向量入库操作
                vectorStore.accept(chunkedDocuments);
                
                System.out.println("[RAG Pipeline] Successfully ingested " + chunkedDocuments.size() + " vectors.");
                context.setNodeOutput(this.operatorId, true);
                return true;
            } catch (Exception e) {
                System.err.println("[RAG Pipeline] Database Connection/Embedding failed at Vectorization phase: " + e.getMessage());
                // 这里记录异常断点
                context.setNodeOutput(this.operatorId + "_ERROR", e.getMessage());
                return false;
            }
        });
    }
}
