package com.dbgpt.java.core.agent.profile;

import lombok.Data;
import lombok.Builder;

import java.util.List;

/**
 * 对应 DB-GPT Python 源码中的 AgentProfile
 * 包含智能体的人设、职责、能力边界等
 */
@Data
@Builder
public class AgentProfile {
    private String name;               // 智能体名称
    private String profile;            // 核心设定词
    private String goal;               // 智能体目标
    private String constraints;        // 限制条件/守则
    private String desc;               // 功能描述
    private List<String> toolNames;    // 挂载的底层工具集合
}