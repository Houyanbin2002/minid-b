package com.dbgpt.java.core.skill;

import java.util.Map;

/**
 * 对应 DB-GPT Python 源码的 Skill/Tool 抽象
 * 代表一个可执行的具体能力 (如查表、调用MCP接口等)
 */
public interface BaseSkill {

    /**
     * 工具的全局唯一名称
     */
    String getName();

    /**
     * 该工具的用途描述（提供给大模型看）
     */
    String getDescription();

    /**
     * 执行技能，返回 JSON 或文本结果
     * @param parameters LLM 传递进来的解析参数
     */
    String execute(Map<String, Object> parameters);
}
