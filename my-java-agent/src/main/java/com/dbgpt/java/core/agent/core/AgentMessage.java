package com.dbgpt.java.core.agent.core;

import lombok.Data;
import lombok.Builder;

import java.util.Map;

/**
 * 等价于 DB-GPT 中的 Action / Message 对象
 * 记录多智能体之间传递的上下文，包括思考过程、动作和观察结果
 */
@Data
@Builder
public class AgentMessage {
    private String role; // 角色，如 user, assistant, system, tool
    private String content; // 文本内容
    private String name; // 发送者/接收者的具体名称
    private String actionName; // 触发的工具名称 (若有)
    private String actionInput; // 工具执行的入参
    private String observation; // 工具执行的结果返回
    
    // DB-GPT 特有的上下文透传
    private Map<String, Object> context; 
}