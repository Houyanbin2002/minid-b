package com.dbgpt.java.core.rag.pipeline.operators;

import com.dbgpt.java.core.awel.dag.DAGContext;
import com.dbgpt.java.core.awel.operator.BaseOperator;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 对应 DB-GPT 的 DocumentSplitterOperator
 * 职责：将解析好的大文本，根据 Token 容量，进行带 Overlap 的滑窗切割，防止语义截断
 */
public class DocumentChunkingOperator extends BaseOperator<List<Document>, List<Document>> {

    private final TokenTextSplitter splitter;

    public DocumentChunkingOperator(String operatorId, int chunkSize, int stepSize) {
        super(operatorId, "DynamicChunking");
        // 配置 Spring AI 的原生切块器 (对应 DB-GPT 的 Chunking)
        // chunkSize = 块最大的 token 数，stepSize = 步长，决定了 overlap 并集有多大
        this.splitter = new TokenTextSplitter(chunkSize, stepSize, 5, 20000, true);
    }

    @Override
    public CompletableFuture<List<Document>> call(List<Document> rawDocuments, DAGContext context) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("[RAG Pipeline] Node 2 - Chunking " + rawDocuments.size() + " large documents...");
            
            // 执行动态切片 (比如把几万字的运动营养学 PDF 劈成几十个小页)
            List<Document> chunkedDocs = splitter.apply(rawDocuments);
            System.out.println("[RAG Pipeline] Split into " + chunkedDocs.size() + " individual chunks base on token limit.");
            
            context.setNodeOutput(this.operatorId, chunkedDocs);
            return chunkedDocs;
        });
    }
}
