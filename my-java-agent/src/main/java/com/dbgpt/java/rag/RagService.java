package com.dbgpt.java.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * RAG 服务：负责加载文档、切分(Chunking)、向量化(Embedding)并注入域(Domain)级别的 Metadata 保障知识边界。
 */
@Service
public class RagService {

    private final VectorStore vectorStore;

    // 采用与 DB-GPT 默认相近的滑动窗口块切分策略
    private final TokenTextSplitter textSplitter = new TokenTextSplitter(800, 350, 5, 10000, true);

    public RagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 核心强化：支持传入 domain 标签（对应 DB-GPT 的 Space）的入库方法。
     * @param fileResource 资源文件（如《营养学指南.pdf》）
     * @param domain 知识领域标记（如 "Nutrition", "Rehabilitation", "VenueRule"）
     */
    public void ingestDocument(Resource fileResource, String domain) {
        // 1. Tika Load (支持 PDF, Excel, txt 等多种企业格式)
        TikaDocumentReader documentReader = new TikaDocumentReader(fileResource);
        List<Document> documents = documentReader.get();

        // 2. 将每份解析出的根文件的全局 metadata 里强制打上域标签
        documents.forEach(doc -> {
            Map<String, Object> meta = doc.getMetadata();
            meta.put("space_domain", domain); 
        });

        // 3. Split (按 Token 语义切片，子切片会自动继承父级的 space_domain 标签)
        List<Document> splitDocuments = textSplitter.apply(documents);

        // 4. Embedding & Milvus Store
        vectorStore.accept(splitDocuments);
        System.out.println("[RAG Service] 成功将 " + splitDocuments.size() + " 个片段存入 Milvus。隔离域(Domain): " + domain);
    }

    /**
     * RAG 相似度召回 (提供给未通过 Tool 而是直接底层调用的备用接口)
     */
    public List<Document> retrieve(String query, int topK, double threshold) {
        SearchRequest request = SearchRequest.query(query).withTopK(topK).withSimilarityThreshold(threshold);
        return vectorStore.similaritySearch(request);
    }
}
