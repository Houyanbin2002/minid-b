package com.dbgpt.java.core.agent.parser;

import lombok.Data;

/**
 * 对应 DB-GPT 中的 Action 解析实体
 * 记录 LLM 决定要调用的工具以及参数
 */
@Data
public class ParsedAction {
    private String actionName;      // 比如 "InBodyReader"
    private String actionInput;     // 比如 "{\\"userId\\": \\"12345\\"}"
    private String thought;         // 模型的思考过程
    private String finalAnswer;     // 如果不需要调用工具，直接返回的最终答案
    private boolean isFinished;     // 判断是否结束工具调用循环
}
