package com.dbgpt.java.fitness.skills;

import com.dbgpt.java.core.skill.BaseSkill;
import java.util.Map;

/**
 * 【模拟实现】场馆会员卡系统操作工具
 * 模拟对健身房自有 CMS/CRM 系统进行读写操作，用于查卡及退费/升级预估
 */
public class VenueCardSkill implements BaseSkill {

    @Override
    public String getName() {
        return "venue_card_operations";
    }

    @Override
    public String getDescription() {
        return "用于查询用户的会员卡状态、剩余天数、会员等级，以及进行卡务相关的操作（如挂失查询）。必须输入参数JSON，包含: user_id (用户ID或手机号)。";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        String userId = (String) parameters.getOrDefault("user_id", "unknown");

        System.out.println("[Skill Execution] 正在连接场馆内 CRM 系统查询卡务信息, 用户：" + userId);

        if ("unknown".equals(userId)) {
            return "{\"error\": \"查询失败，需要提供具体的 user_id。\"}";
        }

        // Mock 返回当前用户的卡信息
        return String.format("""
            {
              "user_id": "%s",
              "card_type": "VIP_ANNUAL_PASS",
              "status": "active",
              "register_date": "2025-10-01",
              "expire_date": "2026-10-01",
              "remaining_days": 190,
              "coach_assigned": "Coach_Mike",
              "can_transfer": true,
              "can_refund": false
            }
            """, userId);
    }
}
