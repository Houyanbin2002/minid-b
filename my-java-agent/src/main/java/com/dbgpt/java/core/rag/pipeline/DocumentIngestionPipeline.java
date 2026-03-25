package com.dbgpt.java.core.rag.pipeline;

import com.dbgpt.java.core.awel.dag.DAGContext;
import com.dbgpt.java.core.rag.pipeline.operators.DocumentChunkingOperator;
import com.dbgpt.java.core.rag.pipeline.operators.TikaDocumentLoaderOperator;
import com.dbgpt.java.core.rag.pipeline.operators.VectorStoreIngestionOperator;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 等价于 DB-GPT 的 DocumentIngestion DAG Pipeline
 * 组装文档入库的全生命周期流水线：解析 -> 分片 -> Embedding 入库
 */
@Service
public class DocumentIngestionPipeline {

    private final VectorStore vectorStore;

    public DocumentIngestionPipeline(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 执行这套可观测、带有上下文打点的断点入库 DAG 流
     */
    public CompletableFuture<Boolean> startIngestion(Resource documentResource) {
        // 1. 初始化属于这个离线流水线的独立大上下文 (用来追踪各节点成功与否)
        DAGContext context = new DAGContext();
        String runId = context.getRunId();
        System.out.println("Starting Ingestion Pipeline. DAG Run [ID: " + runId + "]");

        // 2. 声明算子 (物理上它们独立且可替换，代表了各生命周期)
        var loaderNode = new TikaDocumentLoaderOperator("Node_Tika_Loader");
        // 配置动态分分块: 最大段落 800 Token，段落间重叠度 300 Token (避免语句截断)
        var chunkerNode = new DocumentChunkingOperator("Node_Token_Chunker", 800, 300);
        var milvusNode = new VectorStoreIngestionOperator("Node_Milvus_Saver", vectorStore);

        // 3. 构建 CompletableFuture 响应式流水线 (平替 Python 的 `asyncio` Flow)
        return loaderNode.call(documentResource, context)
                .thenComposeAsync(rawDocs -> {
                    // 如果拉取数据失败，可在上下文中提取并断点
                    if (context.getNodeOutput("Node_Tika_Loader_ERROR") != null) {
                        return CompletableFuture.completedFuture(null);
                    }
                    // 把获取到的数据送到 分片节点
                    return chunkerNode.call(rawDocs, context);
                })
                .thenComposeAsync(chunkedDocs -> {
                    if (chunkedDocs == null) return CompletableFuture.completedFuture(false);
                    // 切片完成，把分好的数组塞进 向量化节点
                    return milvusNode.call(chunkedDocs, context);
                })
                .thenApply(finalStatus -> {
                    if (context.getNodeOutput("Node_Milvus_Saver_ERROR") != null) {
                        System.err.println("Pipeline failed at Vectorization stage.");
                        return false;
                    }
                    return finalStatus;
                });
    }
}
