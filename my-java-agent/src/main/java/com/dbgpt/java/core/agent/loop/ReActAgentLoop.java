package com.dbgpt.java.core.agent.loop;

import com.dbgpt.java.core.agent.parser.ParsedAction;
import com.dbgpt.java.core.agent.parser.ReActOutputParser;
import com.dbgpt.java.core.prompt.PromptTemplate;
import com.dbgpt.java.core.prompt.ReActPromptConfig;
import com.dbgpt.java.core.skill.dispatcher.DBGPTSkillDispatcher;
import com.dbgpt.java.core.skill.BaseSkill;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 顶级复刻：重现 DB-GPT 中核心的 Agent 执行大循环 (Thought -> Action -> Observation)
 * 这是 Agent 能表现得像“智能体”的绝对中枢代码
 */
public class ReActAgentLoop {

    private final ChatClient chatClient;
    private final DBGPTSkillDispatcher skillDispatcher;
    private final ReActOutputParser parser = new ReActOutputParser();
    private final int maxIterations = 5;
    private final List<String> agentAllowedTools; // 当前Agent配置允许的专属工具列表

    public ReActAgentLoop(ChatClient chatClient, DBGPTSkillDispatcher skillDispatcher, List<String> agentAllowedTools) {
        this.chatClient = chatClient;
        this.skillDispatcher = skillDispatcher;
        this.agentAllowedTools = agentAllowedTools;
    }

    /**
     * 对应 DB-GPT Python: run() / _execute()
     */
    public String run(String userQuestion) {

        // 1. 根据当前用户的输入，动态从本 Agent 被分配的工具池中，使用向量检索召回 Top 3 最相关工具
        List<BaseSkill> topSkills = skillDispatcher.retrieveTopSkillsForAgent(agentAllowedTools, userQuestion, 3);

        // 2. 初始化模板和记忆便签(scratchpad)
        PromptTemplate template = new PromptTemplate("ReAct", ReActPromptConfig.REACT_PROMPT_TEMPLATE);
        StringBuilder scratchpad = new StringBuilder();

        Map<String, String> vars = new HashMap<>();
        // 这里传入的已经是经过向量检索后精简过的少数工具，防止撑爆大模型 Context
        vars.put("tool_descriptions", skillDispatcher.getDynamicToolsDescription(topSkills));
        vars.put("tool_names", skillDispatcher.getDynamicToolNames(topSkills));
        vars.put("input", userQuestion);

        int currentIter = 0;

        // 3. 也是 ReAct 反思调用循环
        while (currentIter < maxIterations) {
            vars.put("agent_scratchpad", scratchpad.toString());

            // a). 把当前带工具说明的 Prompt 喂给大模型(含之前的工具产出历史)
            String prompt = template.format(vars);
            String llmResponse = chatClient.prompt().system(prompt).call().content();

            // b). 解析输出 (找 Action)
            ParsedAction action = parser.parse(llmResponse);

            // c). 递归中止判断：如果 LLM 说"Final Answer:" 也就完成了任务
            if (action.isFinished()) {
                return action.getFinalAnswer();
            }

            // d). 执行 Action 并获取 Observation (即 DB-GPT 里的 Plugin 执行)
            String observation = skillDispatcher.dispatch(action.getActionName(), action.getActionInput());

            // e). 把历史记入便签，让模型下个回合阅读
            scratchpad.append(llmResponse).append("\nObservation: ").append(observation).append("\nThought: ");

            currentIter++;
        }

        return "Error: Agent stopped after reaching max iterations (" + maxIterations + ").";
    }
}