package com.dbgpt.java.core.rag.pipeline.operators;

import com.dbgpt.java.core.awel.dag.DAGContext;
import com.dbgpt.java.core.awel.operator.BaseOperator;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 对应 DB-GPT 的 DocumentLoaderOperator (RAG 流水线的起点)
 * 职责：接入原始数据源（PDF、Word课件等），利用 Apache Tika 进行解析
 */
public class TikaDocumentLoaderOperator extends BaseOperator<Resource, List<Document>> {

    public TikaDocumentLoaderOperator(String operatorId) {
        super(operatorId, "TikaDocumentLoader");
    }

    @Override
    public CompletableFuture<List<Document>> call(Resource fileResource, DAGContext context) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("[RAG Pipeline] Node 1 - Loading raw document via Apache Tika: " + fileResource.getFilename());
            try {
                // Apache Tika 解析底层复杂的期刊/SOP格式
                TikaDocumentReader reader = new TikaDocumentReader(fileResource);
                List<Document> documents = reader.get();
                
                context.setNodeOutput(this.operatorId, documents);
                return documents;
            } catch (Exception e) {
                // 断点和重试可在此基于上下文标记失败状态
                context.setNodeOutput(this.operatorId + "_ERROR", e.getMessage());
                throw new RuntimeException("Tika parsing failed: " + e.getMessage(), e);
            }
        });
    }
}
