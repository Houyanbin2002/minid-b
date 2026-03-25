package com.dbgpt.java.core.awel.engine;

import com.dbgpt.java.core.agent.core.AgentMessage;
import com.dbgpt.java.core.agent.core.ConversableAgent;
import com.dbgpt.java.core.awel.router.DynamicCopilotRouter;
import com.dbgpt.java.core.awel.router.WorkflowPlan;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 等价于 DB-GPT 的 AgentManager / MultiAgentRunner
 * 这是一个中央调度器，结合上面手写的动态 Copilot Router，实现"按需执行、自由拼装"的工作流。
 */
public class DynamicWorkflowEngine {

    private final DynamicCopilotRouter router;
    private final Map<String, ConversableAgent> registeredAgents = new HashMap<>();

    public DynamicWorkflowEngine(DynamicCopilotRouter router) {
        this.router = router;
    }

    public void registerAgent(ConversableAgent agent) {
        this.registeredAgents.put(agent.getName(), agent);
    }

    /**
     * 核心路由与执行引擎
     * 接收用户的动态需求，大模型决定调用几个人，然后再跑。
     */
    public CompletableFuture<String> executeUserRequest(String userRequest) {
        
        // 1. 调用 Router，让上帝视角的大模型根据需求生成这次的动态 DAG 计划
        List<String> availableAgentNames = List.copyOf(registeredAgents.keySet());
        WorkflowPlan plan = router.buildDynamicWorkflow(userRequest, availableAgentNames);
        
        System.out.println("---- Dynamic Plan Generated ----");
        System.out.println("Intent: " + plan.getUserIntent());
        
        // 【注：下面是一个简化的动态调度执行器】
        // 为了平替您要求的"有些时候只用其中一个"，大模型生成的 DAG 列表中，就只会包含那个被选中的 Agent。
        // DB-GPT 本核里是通过 resolve dependencies 拓扑排序后并行执行的
        
        CompletableFuture<String> finalContext = CompletableFuture.completedFuture("User Request: " + userRequest);

        // 简略串联：根据大模型编好的列表，逐个击破 (若有依赖并行的我们可以用到之前的 JoinOperator)
        for (WorkflowPlan.AgentNode node : plan.getExecutionDAG()) {
            ConversableAgent currentAgent = registeredAgents.get(node.getAgentName());
            if (currentAgent == null) continue;

            System.out.println("-> Dispatching task to: [" + currentAgent.getName() + "] Reason: " + node.getReason());

            finalContext = finalContext.thenComposeAsync(contextMsg -> {
                AgentMessage msg = AgentMessage.builder()
                        .role("user")
                        .content(contextMsg)
                        .build();

                // DB-GPT的通信协议：让 Agent 处理信息
                return currentAgent.receive(msg, null) // 在真实的系统中 sender 会是 user proxy
                        .thenApply(v -> {
                            // 从 L1 内存里拔出他刚刚计算出的思考结果传给下一个节点
                            List<AgentMessage> mems = currentAgent.getMemory().getMessages();
                            return "[Output from " + currentAgent.getName() + "]:\\n" + mems.get(mems.size() - 1).getContent();
                        });
            });
        }

        return finalContext;
    }
}
