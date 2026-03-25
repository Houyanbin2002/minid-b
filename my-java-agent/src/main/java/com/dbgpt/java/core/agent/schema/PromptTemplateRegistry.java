package com.dbgpt.java.core.agent.schema;

import java.util.HashMap;
import java.util.Map;

/**
 * 对应 DB-GPT Python 源码中的 Prompt 体系：
 * packages/dbgpt-core/src/dbgpt/core/schema/prompt.py 或 packages/dbgpt-core/src/dbgpt/core/prompt/
 * 用于系统级地管辖各类 Agent 以及 Workflow 所产生的系统设定、结构化Prompt范式
 */
public class PromptTemplateRegistry {

    private final Map<String, String> templates = new HashMap<>();

    public PromptTemplateRegistry() {
        // 预设几组符合 DB-GPT 风格的母版
        
        // 1. 标准 ReAct
        templates.put("REACT_SYSPROMPT",
            "You are an expert AI assistant.\n" +
            "You should answer the problem explicitly with thoughts, actions, observations, and final answers.\n" +
            "Available tools: {tools}\n" +
            "Strictly follow:\n" +
            "Question: the input question you must answer\n" +
            "Thought: you should always think about what to do\n" +
            "Action: the action to take, should be one of [{tool_names}]\n" +
            "Action Input: the input to the action\n" +
            "Observation: the result of the action\n" +
            "... (this Thought/Action/Action Input/Observation can repeat N times)\n" +
            "Thought: I now know the final answer\n" +
            "Final Answer: the final answer to the original input question\n");
            
        // 2. 动态路由调度
        templates.put("ROUTER_PROMPT",
            "You are a sophisticated dispatch engine (Router Protocol).\n" +
            "Based on the user's input: '{user_input}', \n" +
            "Select the best single Agent to handle the situation out of: {agents_list}.\n" +
            "Return only the exact exact name of the chosen agent.");
    }

    public void registerNewTemplate(String key, String template) {
        templates.put(key, template);
    }

    public String getTemplate(String key) {
        return templates.getOrDefault(key, "You are a helpful AI assistant. Please respond politely.");
    }
    
    // 动态格式化
    public String render(String key, Map<String, String> kwargs) {
        String base = getTemplate(key);
        for (Map.Entry<String, String> entry : kwargs.entrySet()) {
            base = base.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return base;
    }
}
