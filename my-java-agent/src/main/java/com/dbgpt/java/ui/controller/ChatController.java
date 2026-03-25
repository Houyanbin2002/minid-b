package com.dbgpt.java.ui.controller;

import com.dbgpt.java.cache.SemanticCacheManager;
import com.dbgpt.java.intent.DBGPTIntentRouter;
import com.dbgpt.java.core.agent.core.AgentMessage;
import com.dbgpt.java.core.agent.core.AutoPlanManager;
import com.dbgpt.java.core.agent.core.ConversableAgent;
import com.dbgpt.java.core.memory.AgentMemory;
import com.dbgpt.java.fitness.agents.*;
import com.dbgpt.java.core.skill.dispatcher.DBGPTSkillDispatcher;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Chat Web API (Bridging DB-GPT Multi-Agent system to frontend via SSE)
 * 完美打通意图识别 -> 多Agent编排 -> SSE流式响应的大满贯接口
 */
@RestController
@RequestMapping("/api/v1/chat")
@CrossOrigin(origins = "*") 
public class ChatController {

    private final ChatModel chatModel;
    private final SemanticCacheManager semanticCacheManager;
    private final DBGPTIntentRouter intentRouter;
    private final DBGPTSkillDispatcher skillDispatcher;

    // 假设在全局维护一个短时 Session 记忆 (Demo级别，实战中可基于 SessionID 从Redis拉取)
    private final AgentMemory globalMemory = new AgentMemory();

    @Autowired
    public ChatController(ChatModel chatModel, SemanticCacheManager semanticCacheManager, DBGPTIntentRouter intentRouter, @Autowired(required = false) DBGPTSkillDispatcher skillDispatcher) {
        this.chatModel = chatModel;
        this.semanticCacheManager = semanticCacheManager;
        this.intentRouter = intentRouter;
        // 如果 Spring 容器里没有 dispatcher，就兜底一个默认的防止空指针
        this.skillDispatcher = skillDispatcher != null ? skillDispatcher : new DBGPTSkillDispatcher(null, null);
    }

    /**
     * DB-GPT 标准流式对话接口: /api/v1/chat/stream
     */
    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody Map<String, String> request) {
        String userInput = request.getOrDefault("query", "你好");

        // ==========================================
        // 1. 拦截层：语义缓存 (Semantic Cache) 秒回机制
        // ==========================================
        String cachedAnswer = semanticCacheManager.checkCache(userInput);
        if (cachedAnswer != null) {
            System.out.println("[ChatController] Cached Hit.");
            return Flux.just(cachedAnswer)
                    .map(answer -> formatSse(" [记忆缓存命中，极速返回]\n\n" + answer))
                    .concatWith(Flux.just("data: [DONE]\n\n"));
        }

        // ==========================================
        // 2 & 3. 异步编排接入 (Intent Router) 与 Flux 异步桥接 (打字机效果)
        // ==========================================
        return Flux.create(sink -> {
            // 实时向前端推出“思考状态” - 解决空白等待期的 UX 问题
            sink.next(formatSse(" [网关] 正在深度分析您的意图...\n"));

            CompletableFuture.runAsync(() -> {
                try {
                    // [2.2 核心修复]: 真正接入DBGPTIntentRouter，不再直接用裸大模型聊天
                    List<String> targetAgentNames = intentRouter.recognizeAndRoute(userInput);
                    
                    if (targetAgentNames == null || targetAgentNames.isEmpty()) {
                        // 如果连大模型分类器都分不出来，就派默认专家
                        targetAgentNames = List.of("SummaryAgent"); 
                    }
                    sink.next(formatSse(" [网关] 意图匹配成功！即将唤醒【" + String.join(", ", targetAgentNames) + "】等专家为您解答...\n\n"));

                    // 初始化需要的 Agent 实例集合
                    Map<String, ConversableAgent> activeAgents = initializeAgents(targetAgentNames);
                    
                    AgentMessage userMsg = AgentMessage.builder()
                            .role("user")
                            .name("User")
                            .content(userInput)
                            .build();

                    AgentMessage finalResult;

                    // 判断是否需要多专家协同 (AutoPlan)
                    if (activeAgents.size() > 1) {
                        sink.next(formatSse(" [管家] 触发自动规划！正在将复杂任务拆分并同时派发给多位专家...\n\n"));
                        AutoPlanManager manager = new AutoPlanManager(globalMemory, ChatClient.builder(chatModel), activeAgents);
                        finalResult = manager.send(userMsg, manager).join();
                    } else {
                        // 单路专家直接解答
                        ConversableAgent expert = activeAgents.values().iterator().next();
                        sink.next(formatSse(" [" + expert.getName() + "] 正在独立为您解答...\n\n"));
                        finalResult = expert.receive(userMsg, null).join();
                    }

                    // 沉淀知识并返回给前端
                    globalMemory.appendMessage(userMsg);
                    globalMemory.appendMessage(finalResult);
                    semanticCacheManager.saveCache(userInput, finalResult.getContent());

                    // 分割逐字输出 (打字机效果)
                    String content = finalResult.getContent();
                    for (char c : content.toCharArray()) {
                        sink.next(formatSse(String.valueOf(c)));
                        Thread.sleep(15);
                    }

                    sink.next("data: [DONE]\n\n");
                    sink.complete();

                } catch (Exception e) {
                    sink.next(formatSse("\n\n [网关容错层] 代理网络执行异常: " + e.getMessage()));
                    sink.next("data: [DONE]\n\n");
                    sink.complete();
                }
            });
        });
    }

    private String formatSse(String data) {
        return "data: " + data.replace("\n", "\\n") + "\n\n";
    }

    private Map<String, ConversableAgent> initializeAgents(List<String> agentNames) {
        Map<String, ConversableAgent> map = new HashMap<>();
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        
        for (String name : agentNames) {
            switch (name) {
                case "DataAnalysisAgent" -> map.put(name, new DataAnalysisAgent(globalMemory, builder));
                case "NutritionAgent" -> map.put(name, new NutritionAgent(globalMemory, builder));
                case "WorkoutAgent" -> map.put(name, new WorkoutAgent(globalMemory, builder));
                case "VenueBusinessAgent" -> map.put(name, new VenueBusinessAgent(globalMemory, builder));
                case "SummaryAgent" -> map.put(name, new SummaryAgent(globalMemory, builder));
            }
        }
        return map;
    }
}