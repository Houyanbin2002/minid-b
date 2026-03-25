package com.dbgpt.java.core.agent.core;

import com.dbgpt.java.core.agent.profile.AgentProfile;
import com.dbgpt.java.core.memory.AgentMemory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 等价于 DB-GPT DD-Agents 中的 Manager (Auto-Plan) 自动规划器
 * 核心职责：拆解复杂任务 -> 并行派发给专业 Agent -> 收集结果
 */
public class AutoPlanManager extends ConversableAgent {

    private final ChatClient chatClient;
    private final Map<String, ConversableAgent> professionalAgents;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AutoPlanManager(AgentMemory memory, ChatClient.Builder builder, Map<String, ConversableAgent> professionalAgents) {
        super(
            AgentProfile.builder()
                .name("AutoPlanManager")
                .profile("You are the Chief Manager of a multi-agent system.")
                .goal("Decompose complex user requests into sub-tasks for specific professional agents.")
                .constraints("You do NOT answer questions yourself. Output a JSON plan mapping tasks to agent names.")
                .build(),
            memory
        );
        this.chatClient = builder
                .defaultSystem(this.profile.getProfile() + "\n" + this.profile.getConstraints())
                .build();
        this.professionalAgents = professionalAgents;
    }

    /**
     * 自动规划流程：
     * 1. 拆分任务 (Structured Output)
     * 2. 利用 CompletableFuture 并行 send() 调度
     * 3. 阻塞等待或组合所有结果返回
     */
    @Override
    protected CompletableFuture<AgentMessage> generateReply(AgentMessage message, ConversableAgent sender) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("\n[AutoPlanManager] Received task: " + message.getContent());
            System.out.println("[AutoPlanManager] Thinking & Decomposing tasks...");

            String availableAgents = String.join(", ", professionalAgents.keySet());
            String prompt = String.format("""
                用户请求: '%s'
                可用的专业代理: [%s]
                请将任务拆解。如果不涉及某个代理的专业，不要分配给他。
                必须严格返回纯 JSON 格式：
                {
                  "tasks": [
                    {"agentName": "TargetAgentName", "taskDescription": "What they should do"}
                  ]
                }
                """, message.getContent(), availableAgents);

            String jsonPlan = chatClient.prompt().user(prompt).call().content();
            // 清理可能存在的 markdown 代码块裹挟
            jsonPlan = jsonPlan.replace("```json", "").replace("```", "").trim();

            List<Map<String, String>> tasks;
            try {
                Map<String, Object> parsed = objectMapper.readValue(jsonPlan, Map.class);
                tasks = (List<Map<String, String>>) parsed.get("tasks");
            } catch (Exception e) {
                System.err.println("[AutoPlanManager] Failed to parse plan: " + jsonPlan);
                return AgentMessage.builder().role("assistant").name(getName()).content("Failed to parse Auto-Plan.").build();
            }

            // 核心：利用 CompletableFuture 进行并行派发
            System.out.println("[AutoPlanManager] Executing Auto-Plan in Parallel: " + tasks.size() + " sub-tasks.");
            
            List<CompletableFuture<String>> futures = tasks.stream().map(task -> {
                String targetAgentName = task.get("agentName");
                String taskDesc = task.get("taskDescription");
                ConversableAgent targetAgent = professionalAgents.get(targetAgentName);

                if (targetAgent != null) {
                    System.out.println("   -> Dispatching to [" + targetAgentName + "]: " + taskDesc);
                    AgentMessage subTaskMsg = AgentMessage.builder()
                            .role("user")
                            .name("Manager")
                            .content(taskDesc)
                            .build();

                    // 真正地等待目标 Agent 执行并获取回复，而不是 Mock
                    return targetAgent.receive(subTaskMsg, this)
                            .thenApply(replyMsg -> {
                                String resultContent = replyMsg != null ? replyMsg.getContent() : "No response";
                                return "【" + targetAgentName + " - 执行结果】:\n" + resultContent; 
                            });
                }
                return CompletableFuture.completedFuture("【" + targetAgentName + "】 Error: Agent not found.");
            }).collect(Collectors.toList());

            // 等待所有专业 Agent 的结果并行完成
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            
            // 组装所有结果（此处加入校验和汇总阶段的模拟）
            String aggregatedResults = allOf.thenApply(v -> 
                futures.stream()
                       .map(CompletableFuture::join)
                       .collect(Collectors.joining("\n\n-----------------\n\n"))
            ).join();

            System.out.println("[AutoPlanManager] All sub-tasks completed. Doing verification & aggregation.");
            
            // 校验与聚合可再次调用大模型， 这里简化为拼装后返回
            String finalSummary = String.format("""
                系统已经并行调研了各路专家。以下是他们给出的参考信息综述：
                
                %s
                """, aggregatedResults);

            return AgentMessage.builder()
                    .role("assistant")
                    .name(this.getName())
                    .content(finalSummary)
                    .build();
        });
    }
}