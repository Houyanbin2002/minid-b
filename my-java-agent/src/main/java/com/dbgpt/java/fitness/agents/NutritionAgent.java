package com.dbgpt.java.fitness.agents;

import com.dbgpt.java.core.agent.core.ActionAgent;
import com.dbgpt.java.core.agent.profile.AgentProfile;
import com.dbgpt.java.core.memory.AgentMemory;
import org.springframework.ai.chat.client.ChatClient;
import java.util.List;

/**
 * 营养干预 Agent
 */
public class NutritionAgent extends ActionAgent {

    public NutritionAgent(AgentMemory memory, ChatClient.Builder builder) {     
        super(
            AgentProfile.builder()
                .name("NutritionAgent")
                .profile("你是一位专业的注册营养师。你负责给出科学、健康的营养与饮食建议。")
                .goal("计算用户的 TDEE（每日总消耗），规划日常所需的三大宏量营养素（碳水、蛋白质、脂肪）克数，并基于特定流行的饮食法（如生酮饮食、碳水循环）结合专业文书给出吃法评判。")
                .constraints(
                    "- 在回答饮食法（如生酮、地中海）的专业问题时，必须查阅相关营养学向量知识库，不可完全依靠自身记忆。\n" +
                    "- 在计算三大营养素的克数或热量消耗 TDEE 时，你必须使用提供的计算工具（如 MacroCalculatorSkill）。\n" +
                    "- 可以结合用户的体测分析数据，但不得为用户提供运动训练动作和排期建议等越权操作。\n" +
                    "- 最终结论请采取点状格式呈现（1. 2. 3.）。")
                .toolNames(List.of("MacroCalculatorSkill", "RagRetrievalSkill_Nutrition"))
                .build(),
            memory,
            builder
        );
    }
}
