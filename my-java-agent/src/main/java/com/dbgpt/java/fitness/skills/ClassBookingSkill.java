package com.dbgpt.java.fitness.skills;

import com.dbgpt.java.core.skill.BaseSkill;
import java.util.Map;

/**
 * 【模拟实现】课程预约工具
 * 模拟操作系统，用于检索当周团课、私教课，以及执行占座命令
 */
public class ClassBookingSkill implements BaseSkill {

    @Override
    public String getName() {
        return "class_booking_service";
    }

    @Override
    public String getDescription() {
        return "用于查询某天的健身房课程排班表（如瑜伽、搏击、私教日程），或者执行课程预约。传入参数必须是JSON，包含：action_type (可以是 query_class 或 book_class), date (日期，形如YYYY-MM-DD), class_type (课程类型可选，如 yoga/boxing/pt)。";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        String actionType = (String) parameters.getOrDefault("action_type", "query_class");
        String date = (String) parameters.getOrDefault("date", "2026-03-25");
        String classType = (String) parameters.getOrDefault("class_type", "all");

        System.out.println("[Skill Execution] 触发课程服务, 操作：" + actionType + " 日期：" + date + " 类型：" + classType);

        if ("book_class".equals(actionType)) {
            // 模拟预定逻辑
            return "{\"status\": \"success\", \"message\": \"" + date + " 的 " + classType + " 课程预约成功，已扣除1个课时。\"}";
        } else {
            // 模拟查询排班逻辑
            if (classType.contains("yoga") || classType.contains("瑜伽")) {
                return """
                    [
                      {"time": "10:00-11:00", "class_name": "流瑜伽", "coach": "Alice", "available_slots": 5},
                      {"time": "19:00-20:00", "class_name": "阴瑜伽", "coach": "Alice", "available_slots": 1}
                    ]
                    """;
            } else if (classType.contains("pt") || classType.contains("私教")) {
                 return """
                    [
                      {"time": "14:00-15:00", "class_name": "一对一增肌指导", "coach": "Mike", "available_slots": 1},
                      {"time": "16:00-17:00", "class_name": "拉伸康复", "coach": "John", "available_slots": 1}
                    ]
                    """;
            } else {
                 return """
                    [
                      {"time": "10:00-11:00", "class_name": "流瑜伽", "coach": "Alice", "available_slots": 5},
                      {"time": "18:30-19:30", "class_name": "莱美搏击操", "coach": "Bob", "available_slots": 12},
                      {"time": "19:00-20:00", "class_name": "阴瑜伽", "coach": "Alice", "available_slots": 1}
                    ]
                    """;
            }
        }
    }
}
