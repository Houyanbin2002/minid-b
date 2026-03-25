package com.dbgpt.java.fitness.agents;

import com.dbgpt.java.core.agent.core.ActionAgent;
import com.dbgpt.java.core.agent.profile.AgentProfile;
import com.dbgpt.java.core.memory.AgentMemory;
import com.dbgpt.java.core.skill.dispatcher.DBGPTSkillDispatcher;
import org.springframework.ai.chat.client.ChatClient;
import java.util.List;

/**
 * 您的核心业务：场馆内业务 Agent
 * 职责：预约教练（根据用户的健身需求推荐教练）、查询健身卡到期时间、续卡政策解答等。
 */
public class VenueBusinessAgent extends ActionAgent {

    public VenueBusinessAgent(AgentMemory memory, ChatClient.Builder builder, DBGPTSkillDispatcher dispatcher) {
        super(
            AgentProfile.builder()
                .name("VenueBusinessAgent")
                .profile("你是一位热情的场馆前台与运营管家。负责专门处理与“钱、课、店”等有关的门店商业服务问题。")
                .goal("解答诸如'你们店在哪'、'有什么次卡'、'帮我约今晚的莱美单车课' 等问题。")
                .constraints(
                    "- 在回答相关购课、场馆服务或课表时，必须使用系统的 VenueCardSkill（门店卡项流转系统读取）或 ClassBookingSkill（自动约课API）。\n" +
                    "- 绝对不为用户解答如何健身、如何吃饭等功能性问题。\n" +
                    "- 输出应直接、清晰反映出查询的场地状态或约课结果状态。")
                .toolNames(List.of("VenueCardSkill", "ClassBookingSkill"))
                .build(),
            memory,
            builder,
            dispatcher
        );
    }
}
