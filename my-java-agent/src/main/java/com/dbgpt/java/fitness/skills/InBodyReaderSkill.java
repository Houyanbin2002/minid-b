package com.dbgpt.java.fitness.skills;

import com.dbgpt.java.core.skill.BaseSkill;
import java.util.Map;

/**
 * 【模拟实现】体测仪数据读取工具
 * 模拟调用外部 InBody SaaS 服务器的 API，获取用户的身体成分数据
 */
public class InBodyReaderSkill implements BaseSkill {

    @Override
    public String getName() {
        return "read_inbody_data";
    }

    @Override
    public String getDescription() {
        return "用于根据用户的会员ID或手机号，读取他最近一次的InBody体测分析数据。包含体脂率、骨骼肌、水分等。输入参数应当是一个包含 'user_id' 的JSON。";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        String userId = (String) parameters.getOrDefault("user_id", "unknown");
        
        System.out.println("[Skill Execution] 正在通过 HTTP 请求接入 InBody SaaS 接口获取用户 " + userId + " 的数据...");

        // 这里模拟真实的 API 返回结果，实际业务中应该发送 restTemplate 或 okhttp 请求
        if ("unknown".equals(userId)) {
            return "{\"error\": \"未提供 user_id 导致无法查询体测数据\"}";
        }

        // 模拟固定返回
        return """
            {
               "user_id": "%s",
               "test_date": "2026-03-24",
               "height_cm": 175.5,
               "weight_kg": 78.2,
               "body_fat_percentage": 24.5,
               "skeletal_muscle_kg": 32.1,
               "basal_metabolic_rate": 1650,
               "health_score": 72,
               "analysis": "体脂率偏高（标准15-20%%），肌肉量达标，建议主要进行减脂干预"
            }
            """.formatted(userId);
    }
}
