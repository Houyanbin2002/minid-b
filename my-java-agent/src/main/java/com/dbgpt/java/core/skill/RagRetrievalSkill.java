package com.dbgpt.java.core.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("ask_knowledge_base")
@Description("当你需要了解未知的、针对特定领域的专业知识、最新指南或参考资料时，请调用此工具。")
public class RagRetrievalSkill implements Function<RagRetrievalSkill.RagRequest, String> {

    private final VectorStore vectorStore;

    public RagRetrievalSkill(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public record RagRequest(
            @JsonProperty(value = "query", required = true)
            @JsonPropertyDescription("需要检索的具体问题（自然语言），如：生酮饮食的副作用是什么")
            String query,

            @JsonProperty(value = "domain", required = true)
            @JsonPropertyDescription("要查询的知识库领域，可选值：Nutrition, Workout, Venue")
            String domain
    ) {}

    @Override
    public String apply(RagRequest request) {
        String query = request.query();
        String domain = request.domain();
        if (query == null || query.isBlank()) {
            return "{\"error\": \"必须提供查询内容 query\"}";
        }

        System.out.println("[Skill Execution] 触发 RAG 从全局库中抽取 [" + domain + "] 领域的专业知识: " + query);

        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();

            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.query(query)
                                 .withTopK(3)
                                 .withSimilarityThreshold(0.70)
                                 .withFilterExpression(b.eq("space_domain", domain).build())
            );

            if (documents == null || documents.isEmpty()) {
                return "没有在【" + domain + "】知识库中找到关于该问题的内容，请基于你的常识回答或告知用户不清楚。";
            }

            String ragContext = documents.stream()
                    .map(doc -> "- " + doc.getContent())
                    .collect(Collectors.joining("\n\n"));

            return "在你检索的【" + domain + "】领域知识库中找到以下权威参考：\n" + ragContext;

        } catch (Exception e) {
             return "{\"error\": \"向量检索失败\"}";
        }
    }
}
