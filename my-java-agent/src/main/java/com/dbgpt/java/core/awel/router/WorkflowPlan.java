package com.dbgpt.java.core.awel.router;

import com.dbgpt.java.core.agent.profile.AgentProfile;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流动态路由计划
 */
@Data
public class WorkflowPlan {
    private String userIntent;            // 核心意图概述
    private List<AgentNode> executionDAG; // 要执行的智能体节点 (代表执行顺序)

    @Data
    public static class AgentNode {
        private String agentName;         // 挑中的智能体名称
        private String reason;            // 为什么挑它？
        private List<String> dependencies;// 依赖哪些智能体的输出 (用于构建并发流程或串行)
    }
}
