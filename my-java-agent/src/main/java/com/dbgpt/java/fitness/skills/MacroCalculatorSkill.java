package com.dbgpt.java.fitness.skills;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service("calculate_macros")
@Description("用于计算用户每日总消耗热量(TDEE)以及核心营养素的克数分布(碳酸/蛋白质/脂肪)。根据身高体重、活动量和目标(减脂/增肌/维稳)输出详细的 JSON 数据。")
public class MacroCalculatorSkill implements Function<MacroCalculatorSkill.MacroRequest, MacroCalculatorSkill.MacroResponse> {

    public record MacroRequest(
            @JsonProperty(value = "weight_kg", defaultValue = "70", required = true)
            @JsonPropertyDescription("体重(公斤), 例如: 70")
            double weightKg,

            @JsonProperty(value = "h_cm", defaultValue = "170", required = true)
            @JsonPropertyDescription("身高(厘米), 例如: 170")
            double hCm,

            @JsonProperty(value = "age", defaultValue = "30", required = true)
            @JsonPropertyDescription("年龄, 例如: 30")
            int age,

            @JsonProperty(value = "gender", defaultValue = "男", required = true)
            @JsonPropertyDescription("性别 (男/女), 默认男")
            String gender,

            @JsonProperty(value = "activity_level", defaultValue = "light", required = true)
            @JsonPropertyDescription("活动水平: sedentary, light, moderate, active, very_active")
            String activityLevel,

            @JsonProperty(value = "goal", defaultValue = "fat_loss", required = true)
            @JsonPropertyDescription("目标: fat_loss(减脂), maintain(维稳), muscle_gain(增肌)")
            String goal
    ) {}

    public record MacroResponse(
            double bmrKcal,
            double tdeeKcal,
            double targetCaloriesKcal,
            MacrosGrams macrosGrams,
            String error
    ) {
        public record MacrosGrams(double protein, double fat, double carbs) {}
    }

    @Override
    public MacroResponse apply(MacroRequest request) {
        try {
            double weight = request.weightKg() > 0 ? request.weightKg() : 70.0;
            double height = request.hCm() > 0 ? request.hCm() : 170.0;
            int age = request.age() > 0 ? request.age() : 30;
            String gender = request.gender() != null ? request.gender() : "男";
            String activityLevel = request.activityLevel() != null ? request.activityLevel() : "light";
            String goal = request.goal() != null ? request.goal() : "fat_loss";

            System.out.println("[Skill Execution] 触发宏量营养素计算器: W:" + weight + " H:" + height + " G:" + goal);

            double bmr = (10 * weight) + (6.25 * height) - (5 * age);
            bmr += gender.contains("女") ? -161 : 5;

            double activityMultiplier = switch (activityLevel.toLowerCase()) {
                case "sedentary" -> 1.2;
                case "light" -> 1.375;
                case "moderate" -> 1.55;
                case "active" -> 1.725;
                case "very_active" -> 1.9;
                default -> 1.375;
            };
            double tdee = bmr * activityMultiplier;

            double targetCalories = tdee;
            if (goal.contains("fat_loss") || goal.contains("减脂")) {
                targetCalories -= 500;
            } else if (goal.contains("muscle_gain") || goal.contains("增肌")) {
                targetCalories += 300;
            }

            double proteinPerKg = goal.contains("fat_loss") ? 2.2 : 2.0;
            double proteinGrams = weight * proteinPerKg;
            double proteinCals = proteinGrams * 4;

            double fatCals = targetCalories * 0.25;
            double fatGrams = fatCals / 9;

            double carbCals = targetCalories - proteinCals - fatCals;
            double carbGrams = Math.max(carbCals / 4, 0);

            return new MacroResponse(
                    Math.round(bmr), 
                    Math.round(tdee), 
                    Math.round(targetCalories),
                    new MacroResponse.MacrosGrams(Math.round(proteinGrams), Math.round(fatGrams), Math.round(carbGrams)),
                    null
            );

        } catch (Exception e) {
            return new MacroResponse(0, 0, 0, null, "计算失败: " + e.getMessage());
        }
    }
}
