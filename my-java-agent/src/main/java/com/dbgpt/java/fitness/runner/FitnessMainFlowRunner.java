package com.dbgpt.java.fitness.runner;

import com.dbgpt.java.core.agent.core.ConversableAgent;
import com.dbgpt.java.core.agent.loop.ReActAgentLoop;
import com.dbgpt.java.core.agent.core.AgentMessage;
import com.dbgpt.java.core.memory.AgentMemory;
import com.dbgpt.java.core.skill.dispatcher.DBGPTSkillDispatcher;
import com.dbgpt.java.core.skill.RagRetrievalSkill;
import com.dbgpt.java.fitness.skills.*;
import com.dbgpt.java.fitness.agents.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;

@Component
public class FitnessMainFlowRunner implements CommandLineRunner {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    public FitnessMainFlowRunner(ChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== DB-GPT Java (Spring AI) 自主拆解与动态调度引擎 Init ======");

        ChatClient.Builder builder = ChatClient.builder(chatModel);
        AgentMemory sharedMemory = new AgentMemory();

        DBGPTSkillDispatcher registry = new DBGPTSkillDispatcher();
        registry.registerSkill(new InBodyReaderSkill());
        registry.registerSkill(new ActionLibraryRetrieverSkill());
        registry.registerSkill(new MacroCalculatorSkill());
        registry.registerSkill(new RagRetrievalSkill(vectorStore, "Nutrition"));

        Map<String, ConversableAgent> agentMap = new HashMap<>();
        
        agentMap.put("DataAnalysisAgent", new DataAnalysisAgent(sharedMemory, builder) {
            @Override protected CompletableFuture<AgentMessage> generateReply(AgentMessage message, ConversableAgent sender) {
                return CompletableFuture.supplyAsync(() -> {
                    System.out.println("   ?? [DataAnalysisAgent] 开始执行子任务: " + message.getContent());
                    ReActAgentLoop loop = new ReActAgentLoop(this.chatClient, registry, this.profile.getAllowedTools());
                    return AgentMessage.builder().role("assistant").name(this.getName())
                            .content(loop.run(message.getContent() + "\n你的处理约束: " + this.profile.getConstraints())).build();
                });
            }
        });

        agentMap.put("NutritionAgent", new NutritionAgent(sharedMemory, builder) {
            @Override protected CompletableFuture<AgentMessage> generateReply(AgentMessage message, ConversableAgent sender) {
                return CompletableFuture.supplyAsync(() -> {
                    System.out.println("   ?? [NutritionAgent] 开始执行子任务: " + message.getContent());
                    ReActAgentLoop loop = new ReActAgentLoop(this.chatClient, registry, this.profile.getAllowedTools());
                    return AgentMessage.builder().role("assistant").name(this.getName())
                            .content(loop.run(message.getContent() + "\n你的处理约束: " + this.profile.getConstraints())).build();
                });
            }
        });

        agentMap.put("WorkoutAgent", new WorkoutAgent(sharedMemory, builder) {
            @Override protected CompletableFuture<AgentMessage> generateReply(AgentMessage message, ConversableAgent sender) {
                return CompletableFuture.supplyAsync(() -> {
                    System.out.println("   ?? [WorkoutAgent] 开始执行子任务: " + message.getContent());
                    ReActAgentLoop loop = new ReActAgentLoop(this.chatClient, registry, this.profile.getAllowedTools());
                    return AgentMessage.builder().role("assistant").name(this.getName())
                            .content(loop.run(message.getContent() + "\n你的处理约束: " + this.profile.getConstraints())).build();
                });
            }
        });

        ConversableAgent managerAgent = new ManagerAgent(sharedMemory, builder, agentMap.values());
        ConversableAgent summaryAgent = new SummaryAgent(sharedMemory, builder);

        // --- 测试：仅需单Agent的Case ---
        String singleQuery = "我今天去吃了一顿麦当劳，我不清楚大概热量占比，能不能帮我算一下然后规划我明天的热量？";
        executeAutoPlanningWorkFlow(managerAgent, agentMap, summaryAgent, singleQuery);

        // --- 测试：需多Agent并行执行的Case ---
        String complexQuery = "我是个新手，读一下我的体测数据看看我是算偏胖还是偏瘦？然后我想减脂，给我分别搞个简单的吃饭标准和训练表。";
        executeAutoPlanningWorkFlow(managerAgent, agentMap, summaryAgent, complexQuery);

    }

    private void executeAutoPlanningWorkFlow(ConversableAgent manager, Map<String, ConversableAgent> agentMap, ConversableAgent summaryAgent, String userQuery) {
        System.out.println("\n\n==============================================================");
        System.out.println("???♂? [用户输入]: \"" + userQuery + "\"");
        System.out.println("==============================================================");
        
        // 1. Manager 进行任务拆解 (生成 Plan)
        System.out.println("?? [ManagerAgent] 正在进行意图分析与工作流自动拆解...");
        AgentMessage initialMsg = AgentMessage.builder().role("user").content(userQuery).build();
        
        manager.receive(initialMsg, manager).thenAccept(managerReply -> {
            String plan = managerReply.getContent();
            System.out.println("?? [ManagerAgent 输出了动态执行计划]:\n" + plan);

            // 2. 解析动态返回的拆解结构 [Assign -> XXX]: YYY
            List<CompletableFuture<AgentMessage>> futures = new ArrayList<>();
            Pattern pattern = Pattern.compile("\\[Assign -> (\\w+)\\]:\\s*(.*)");
            Matcher matcher = pattern.matcher(plan);
            
            while (matcher.find()) {
                String agentName = matcher.group(1);
                String subTask = matcher.group(2);
                
                ConversableAgent worker = agentMap.get(agentName);
                if (worker != null) {
                    System.out.println("?? -> 调度 [ " + agentName + " ]...");
                    // 构造给该专家专属的子任务Prompt
                    AgentMessage subMsg = AgentMessage.builder().role("user").content("用户原请求: " + userQuery + "\n你需要处理的部分是: " + subTask).build();
                    futures.add(worker.receive(subMsg, manager));
                }
            }
            
            if(futures.isEmpty()) {
                System.out.println("?? Manager未能识别出有效的子任务。");
                return;
            }

            // 3. 并行执行这些被识别出的子任务 Agent，并在最后聚合
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenAccept(v -> {
                StringBuilder aggregatedContext = new StringBuilder();
                for (CompletableFuture<AgentMessage> future : futures) {
                    try {
                        AgentMessage msg = future.get();
                        aggregatedContext.append("【").append(msg.getName()).append("的结论】:\n");
                        aggregatedContext.append(msg.getContent()).append("\n\n");
                    } catch (Exception e) {}
                }
                
                System.out.println("? [后台并发流水线完成]: " + futures.size() + " 个任务单元已完成，提交给 SummaryAgent 进行渲染整合。");

                AgentMessage sumContextMsg = AgentMessage.builder()
                        .role("user")
                        .content(aggregatedContext.toString())
                        .build();

                summaryAgent.receive(sumContextMsg, summaryAgent).thenAccept(finalReply -> {
                     System.out.println("\n?? [AI 气泡 - 返回给前端的用户视图]:\n" + finalReply.getContent());
                }).join();

            }).join();

        }).join();
    }
}
