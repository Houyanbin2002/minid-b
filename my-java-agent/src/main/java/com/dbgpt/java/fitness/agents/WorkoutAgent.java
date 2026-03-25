package com.dbgpt.java.fitness.agents;

import com.dbgpt.java.core.agent.core.ActionAgent;
import com.dbgpt.java.core.agent.profile.AgentProfile;
import com.dbgpt.java.core.memory.AgentMemory;
import org.springframework.ai.chat.client.ChatClient;
import java.util.List;

/**
 * 训练排课 Agent
 */
public class WorkoutAgent extends ActionAgent {

    public WorkoutAgent(AgentMemory memory, ChatClient.Builder builder) {       
        super(
            AgentProfile.builder()
                .name("WorkoutAgent")
                .profile("你是一位硬核的健身教练和动作专家。你负责一切与健身动作执行、发力技巧以及抗阻训练排期相关的问题。")
                .goal("分析某个动作是否适合用户的现状、解答某块肌肉如何正确发力，或帮用户组合出一套（如周一胸背、周二臀腿）的抗阻/有氧训练排期计划。")
                .constraints(
                    "- 您只能回答关于训练、运动、动作相关的问题。\n" +
                    "- 必须利用 ActionLibraryRetrieverSkill 等动作库检索工具，给出标准视频或图文中的动作要领，而不是随口编造。\n" +
                    "- 如果遇到复杂的伤痛恢复问题或身体功能障碍限制的情况，需要依赖康复学知识库（如 RagRetrievalSkill_Rehabilitation）工具查阅后作答。\n" +
                    "- 切勿指导用户的饮食或场馆订单业务。")
                .toolNames(List.of("ActionLibraryRetrieverSkill", "RagRetrievalSkill_Rehabilitation"))
                .build(),
            memory,
            builder
        );
    }
}
