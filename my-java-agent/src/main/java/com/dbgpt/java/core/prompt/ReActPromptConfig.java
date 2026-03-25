package com.dbgpt.java.core.prompt;

/**
 * 对应 DB-GPT / LangChain 经典的 ReAct Prompt 预设模板
 * 事无巨细：这正是大模型如何被“强制”遵守逻辑走向工具调用的魔法阵
 */
public class ReActPromptConfig {

    public static final String REACT_PROMPT_TEMPLATE = """
            Answer the following questions as best you can. You have access to the following tools:
            
            {tool_descriptions}
            
            Use the following format:
            
            Question: the input question you must answer
            Thought: you should always think about what to do
            Action: the action to take, should be one of [{tool_names}]
            Action Input: the input to the action
            Observation: the result of the action
            ... (this Thought/Action/Action Input/Observation can repeat N times)
            Thought: I now know the final answer
            Final Answer: the final answer to the original input question
            
            Begin!
            
            Question: {input}
            Thought: {agent_scratchpad}
            """;
}

