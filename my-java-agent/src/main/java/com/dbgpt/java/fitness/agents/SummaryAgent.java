package com.dbgpt.java.fitness.agents;

import com.dbgpt.java.core.agent.core.ActionAgent;
import com.dbgpt.java.core.agent.core.AgentMessage;
import com.dbgpt.java.core.agent.core.ConversableAgent;
import com.dbgpt.java.core.agent.profile.AgentProfile;
import com.dbgpt.java.core.memory.AgentMemory;
import org.springframework.ai.chat.client.ChatClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SummaryAgent extends ActionAgent {

    public SummaryAgent(AgentMemory memory, ChatClient.Builder builder) {       
        super(
            AgentProfile.builder()
                .name("SummaryAgent")
                .profile("你是一位富有亲和力、极其温暖的高情商“私域主理人”。你是唯一直接面向用户的最终输出者（前端拼装者）。")
                .goal("你不负责具体的业务计算，而是等前面数据、营养、训练、场馆专家在后台开完小会得出结论后，拿着综合汇报结果，以最高沟通水平为用户排版撰写一份《定制化体型与训练全案》。")
                .constraints(
                    "- 您不能使用任何带有底层业务逻辑的工具，仅仅依赖 AgentMemory（全局记忆状态）的数据收集结论。\n" +
                    "- 输出文案必须体现出有温度、有 Emoji、高情商的属性。\n" +
                    "- 必须用 Markdown 进行美观排版呈现，不可遗漏前面几个专家的关键点结论（体测分析、营养定性、训练规划、场地建议）。")
                .toolNames(List.of())
                .build(),
            memory,
            builder
        );
    }

    @Override
    protected CompletableFuture<AgentMessage> generateReply(AgentMessage message, ConversableAgent sender) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("\n? [SummaryAgent] 正在整合各专家的专业建议，准备为您生成最终全案...");
            
            // 真实还原 DB-GPT 系统提示词层层递进（System -> History -> User）的格式
            String prompt = String.format("""
                基于以下给出的专家诊断结论以及用户的核心诉求，请遵守规范约束，简要回答用户的问题。
                
                已知多方专家诊断内容:
                %s
                
                用户的核心诉求:
                %s
                
                请使用和用户相同的语言（中文）进行综合性的全案整理。""",
                    message.getContent(),
                    this.memory.getVariables().getOrDefault("userQuery", "提供减脂/塑形综合方案"));

            String summaryContent = this.chatClient.prompt()
                    .system(this.profile.getProfile() + "\n" + this.profile.getGoal() + "\n规范约束:\n" + this.profile.getConstraints())
                    .user(prompt)
                    .call()
                    .content();

            return AgentMessage.builder()
                    .role("assistant")
                    .name(this.getName())
                    .content(summaryContent)
                    .build();
        });
    }
}
