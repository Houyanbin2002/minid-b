package com.dbgpt.java.core.awel.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

/**
 * 对应 DB-GPT 的 AgentRouter / PlanningAgent
 * 拥有上帝视角：手里攥着全部可用的 Agent 名单，根据用户的话，"动态"规划出本次对话需要用到哪些Agent，以及它们之间的上下文依赖关系 (即现场动态搭建 DAG)
 */
public class DynamicCopilotRouter {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DynamicCopilotRouter(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * @param userRequest 用户的自然语言输入
     * @param availableAgents 系统里目前注册的所有的 Agent（供模型挑选）
     */
    public WorkflowPlan buildDynamicWorkflow(String userRequest, List<String> availableAgents) {
        String prompt = """
            You are a master workflow planner.
            You have access to the following specialized Agents: %s
            
            Based on the user's request: "%s"
            
            Decide which agents are needed to fulfill this request. You can select ONE agent if it's a simple task, 
            or MULTIPLE agents if it requires complex reasoning. If multiple are selected, define their dependencies 
            (e.g., NutritionAgent depends on DataAnalysisAgent's output).
            
            Respond strictly in the following JSON format without markdown fencing:
            {
               "userIntent": "Brief summary of what user wants",
               "executionDAG": [
                   {
                       "agentName": "NameOfAgent",
                       "reason": "Why needed",
                       "dependencies": ["OtherAgentNameIfAny"]
                   }
               ]
            }
            """;

        String formattedPrompt = String.format(prompt, availableAgents.toString(), userRequest);

        try {
            String jsonOutput = chatClient.prompt().system(formattedPrompt).call().content().trim();
            // 容错: 去除可能有的 markdown 代码块
            jsonOutput = jsonOutput.replaceAll("^```(?:json)?|```$", "").trim();
            
            return objectMapper.readValue(jsonOutput, WorkflowPlan.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to dynamically plan Workflow: " + e.getMessage(), e);
        }
    }
}
