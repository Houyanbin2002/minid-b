package com.dbgpt.java.fitness.skills;

import com.dbgpt.java.core.skill.BaseSkill;
import java.util.Map;

/**
 * 【模拟实现】动作库检索工具
 * 模拟从一个专业的健身动作向量库 (如基于 Milvus + RAG) 或关系型数据库中提取动作
 */
public class ActionLibraryRetrieverSkill implements BaseSkill {

    @Override
    public String getName() {
        return "retrieve_action_library";
    }

    @Override
    public String getDescription() {
        return "根据目标肌群或动作偏好，从健身动作库中检索推荐的训练动作、组数、次数和重量建议。输入参数JSON包括: target_muscle (如胸/背/腿/肩/核心), experience_level (初级/中级/高级)。";
    }

    @Override
    public String execute(Map<String, Object> parameters) {
        String muscle = (String) parameters.getOrDefault("target_muscle", "全身");
        String level = (String) parameters.getOrDefault("experience_level", "初级");

        System.out.println("[Skill Execution] 正在查询动作库 DB，肌群：" + muscle + " 难度：" + level);

        // 使用 switch 进行简单模拟
        if (muscle.contains("胸")) {
            return """
                [
                  {"action": "杠铃平板卧推", "sets": 4, "reps": "8-10", "rest_seconds": 90, "tips": "保持肩胛骨收紧"},
                  {"action": "哑铃上斜卧推", "sets": 3, "reps": "10-12", "rest_seconds": 60, "tips": "推起到顶点不要锁定肘关节"},
                  {"action": "蝴蝶机夹胸", "sets": 3, "reps": "15", "rest_seconds": 45, "tips": "专注肌肉收缩感"}
                ]
                """;
        } else if (muscle.contains("背")) {
            return """
                [
                  {"action": "高位下拉", "sets": 4, "reps": "10-12", "rest_seconds": 60, "tips": "身体微后倾，沉肩"},
                  {"action": "杠铃划船", "sets": 4, "reps": "8-10", "rest_seconds": 90, "tips": "保持腰背挺直"},
                  {"action": "坐姿划船", "sets": 3, "reps": "12", "rest_seconds": 60, "tips": "收缩背阔肌，顶峰停顿一秒"}
                ]
                """;
        } else if (muscle.contains("腿")) {
            return """
                [
                  {"action": "杠铃深蹲", "sets": 4, "reps": "6-8", "rest_seconds": 120, "tips": "膝盖对准脚尖，核心收紧"},
                  {"action": "腿举", "sets": 4, "reps": "10-12", "rest_seconds": 60, "tips": "不要完全伸直锁定膝盖"},
                  {"action": "俯卧腿屈伸", "sets": 3, "reps": "12-15", "rest_seconds": 60, "tips": "控制离心阶段的下放速度"}
                ]
                """;
        } else {
             // 默认全身或核心
            return """
                [
                  {"action": "药球自重深蹲", "sets": 3, "reps": "15", "rest_seconds": 60, "tips": "保持呼吸均匀"},
                  {"action": "平板支撑", "sets": 3, "reps": "60秒", "rest_seconds": 45, "tips": "收紧臀部和核心"},
                  {"action": "俄罗斯挺身", "sets": 3, "reps": "20", "rest_seconds": 45, "tips": "目光跟随双手"}
                ]
                """;
        }
    }
}
