package com.dbgpt.java.core.agent.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 对应 DB-GPT Python 中的 ReActOutputParser (基于 LangChain ReAct 规范)
 * 极致复刻：如何把 LLM 吐出来的长文本，剥离出真实的“意图动作”和“工具入参”
 */
public class ReActOutputParser {

    private static final String ACTION_PATTERN = "Action:\\s*(.*?)\\s*\\nAction Input:\\s*(.*)";
    private static final String FINAL_ANSWER_PATTERN = "Final Answer:\\s*(.*)";

    public ParsedAction parse(String llmOutput) {
        ParsedAction action = new ParsedAction();
        
        // 1. 先匹配是否已经得出了最终结论
        Matcher finalAnswerMatcher = Pattern.compile(FINAL_ANSWER_PATTERN, Pattern.DOTALL).matcher(llmOutput);
        if (finalAnswerMatcher.find()) {
            action.setFinalAnswer(finalAnswerMatcher.group(1).trim());
            action.setFinished(true);
            return action;
        }

        // 2. 如果没结束，提取它想要调用的工具 (Action) 和参数 (Action Input)
        Matcher actionMatcher = Pattern.compile(ACTION_PATTERN, Pattern.DOTALL).matcher(llmOutput);
        if (actionMatcher.find()) {
            action.setActionName(actionMatcher.group(1).trim());
            // DB-GPT 中会清洗部分 markdown 代码块标记，如 ```json {...} ```
            String rawInput = actionMatcher.group(2).trim();
            rawInput = rawInput.replaceAll("^```(?:json)?|```$", "").trim();
            action.setActionInput(rawInput);
            action.setFinished(false);
            return action;
        }

        // 3. Fallback 容错：如果解析失败，直接将原文本作为结果抛出
        action.setFinalAnswer(llmOutput);
        action.setFinished(true);
        return action;
    }
}
