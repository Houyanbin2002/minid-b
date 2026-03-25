package com.dbgpt.java.fitness.agents;

import com.dbgpt.java.core.agent.core.ActionAgent;
import com.dbgpt.java.core.agent.profile.AgentProfile;
import com.dbgpt.java.core.memory.AgentMemory;
import org.springframework.ai.chat.client.ChatClient;
import java.util.List;

/**
 * 体测与体态分析 Agent 
 * 职责：严格基于工具读取用户的身体数据并给出医学/运动学上的解读。
 */
public class DataAnalysisAgent extends ActionAgent {

    public DataAnalysisAgent(AgentMemory memory, ChatClient.Builder builder) {  
        super(
            AgentProfile.builder()
                .name("DataAnalysisAgent")
                .profile("你是一位客观严谨的体测数据分析师与理疗师。")
                .goal("读取用户的历史体侧表现、身体围度，评估用户当前的身体状况是需要减脂、增肌还是塑形，并只提供客观的物理生理结论。")
                .constraints(
                    "- 必须且只能根据提供的数据或者工具返回的数据回答问题。\n" +
                    "- 如果工具返回的信息不足以支持结论，请回答'当前体测数据不完整，无法给出确切分析'，绝对禁止胡乱编造。\n" +
                    "- 输出必须是清晰、客观的指标分析。\n" +
                    "- 绝对不要越权去编造或提供任何具体的健身训练计划、饮食计划！你的工作仅限于生理数据解读与状态评估。")
                .toolNames(List.of("InBodyReaderSkill"))
                .build(),
            memory,
            builder
        );
    }
}
