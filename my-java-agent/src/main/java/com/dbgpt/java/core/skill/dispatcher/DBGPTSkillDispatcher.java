package com.dbgpt.java.core.skill.dispatcher;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 对应 DB-GPT 的 ToolRegistry / PluginDispatcher (Spring AI 适配版)
 * 【模块化/按需加载】：支持依据 Agent Profile 和 User Query 动态通过向量召回相关工具
 */
@Component
public class DBGPTSkillDispatcher {

    private final Map<String, String> skillDescriptions = new ConcurrentHashMap<>();
    private final Map<String, float[]> skillEmbeddings = new ConcurrentHashMap<>();
    private final EmbeddingModel embeddingModel;

    public DBGPTSkillDispatcher(EmbeddingModel embeddingModel, ApplicationContext applicationContext) {
        this.embeddingModel = embeddingModel;
        this.scanAndRegisterSkills(applicationContext);
    }

    /**
     * 系统启动时挂载底层工具，自动扫描 Spring IoC 中实现了 Function 并标有 @Description 的 Bean，
     * 并立即将其 description 进行向量化
     */
    @SuppressWarnings("rawtypes")
    private void scanAndRegisterSkills(ApplicationContext applicationContext) {
        String[] beanNames = applicationContext.getBeanNamesForType(Function.class);
        for (String beanName : beanNames) {
            Description descAnnotation = applicationContext.findAnnotationOnBean(beanName, Description.class);
            if (descAnnotation != null) {
                String desc = descAnnotation.value();
                skillDescriptions.put(beanName, desc);
                System.out.println("[SkillDispatcher] Registering and Embedding tool: " + beanName);
                float[] vector = embeddingModel.embed(desc);
                skillEmbeddings.put(beanName, vector);
            }
        }
    }

    /**
     * 【核心功能】：DB-GPT 真实的工具向量检索逻辑
     * 针对具体的 Agent（它有自己的 bound tools 列表），根据当前用户的 query，通过余弦相似度精准切入最相关的 N 个工具。
     * 可以有效防止把上百个 API 全放进 Prompt 导致的大模型幻觉和上下文超限。
     */
    public List<String> retrieveTopSkillsForAgent(List<String> agentAllowedToolNames, String userQuery, int topK) {
        if (agentAllowedToolNames == null || agentAllowedToolNames.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 获取该 Agent 被允许使用的实际工具集合（必须在我们的注册表中）
        List<String> candidateSkills = agentAllowedToolNames.stream()
                .filter(skillDescriptions::containsKey)
                .collect(Collectors.toList());

        // 如果工具很少（比如 <= topK），不需要走向量检索，直接全量塞给大模型即可
        if (candidateSkills.size() <= topK) {
            return candidateSkills;
        }

        // 2. 如果工具库庞大，给用户的 Query 做 Embedding
        float[] queryVector = embeddingModel.embed(userQuery);

        // 3. 计算该 Agent 辖下所有候选工具的余弦相似度并打分重排
        return candidateSkills.stream()
                .sorted((s1, s2) -> {
                    float[] v1 = skillEmbeddings.get(s1);
                    float[] v2 = skillEmbeddings.get(s2);
                    double score1 = v1 != null ? cosineSimilarity(queryVector, v1) : -1.0;
                    double score2 = v2 != null ? cosineSimilarity(queryVector, v2) : -1.0;
                    return Double.compare(score2, score1); // 降序排序
                })
                .limit(topK)
                .collect(Collectors.toList());
    }

    private double cosineSimilarity(float[] vectorA, float[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
