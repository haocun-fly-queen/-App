package com.eatnotfat.backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.eatnotfat.backend.config.QwenConfig;
import com.eatnotfat.backend.dto.DietPlanRequest;
import com.eatnotfat.backend.dto.ExercisePlanRequest;
import com.eatnotfat.backend.entity.AiLog;
import com.eatnotfat.backend.entity.FoodStandard;
import com.eatnotfat.backend.entity.HealthIndicator;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.mapper.AiLogMapper;
import com.eatnotfat.backend.mapper.FoodStandardMapper;
import com.eatnotfat.backend.vo.DailyPlanResult;
import com.eatnotfat.backend.vo.DietPlanResult;
import com.eatnotfat.backend.vo.RecognizeResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QwenService {

    @Autowired
    private QwenConfig qwenConfig;

    @Autowired
    private FoodStandardMapper foodStandardMapper;

    @Autowired
    private AiLogMapper aiLogMapper;

    @Autowired
    private QwenVLService qwenVLService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 食物识别 ====================

    public RecognizeResult recognizeFood(String imageUrl, String imageBase64, Long userId) {
        long startTime = System.currentTimeMillis();

        try {
            System.out.println("========== 调用通义千问识别食物 ==========");
            System.out.println("userId: " + userId);

            Map<String, Object> requestBody = buildRecognizeRequestBody(imageUrl, imageBase64);
            System.out.println("请求参数: " + JSON.toJSONString(requestBody));

            String response = sendHttpRequest(requestBody, qwenConfig.getVlEndpoint());
            System.out.println("API返回: " + response);

            RecognizeResult result = parseRecognizeResponse(response);
            matchLocalFoods(result);

            long processTime = System.currentTimeMillis() - startTime;
            System.out.println("处理耗时: " + processTime + "ms");

            saveAiLog(userId, imageUrl, imageBase64, "recognize", JSON.toJSONString(result), processTime);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("调用通义千问失败: " + e.getMessage());
        }
    }

    // ==================== 单餐饮食规划 ====================

    public DietPlanResult generateDietPlan(DietPlanRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            System.out.println("========== 调用通义千问饮食规划 ==========");
            System.out.println("用户ID: " + request.getUserId());
            System.out.println("目标餐次: " + request.getTargetMeal().getName());

            String prompt = buildDietPlanPrompt(request);
            System.out.println("规划Prompt长度: " + prompt.length());

            Map<String, Object> requestBody = buildTextRequestBody(prompt, 1500);
            System.out.println("请求参数: " + JSON.toJSONString(requestBody));

            String response = sendHttpRequest(requestBody);
            System.out.println("API返回: " + response);

            DietPlanResult result = parseDietPlanResponse(response, request);

            long processTime = System.currentTimeMillis() - startTime;
            System.out.println("规划耗时: " + processTime + "ms");

            saveAiLog(request.getUserId(), null, null, "diet-plan", JSON.toJSONString(result), processTime);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("AI规划失败，使用本地降级方案");
            return generateFallbackPlan(request);
        }
    }

    private String buildDietPlanPrompt(DietPlanRequest req) {
        DietPlanRequest.UserProfile p = req.getProfile();
        DietPlanRequest.CalorieStatus c = req.getCalorieStatus();
        DietPlanRequest.DietRestrictions r = req.getRestrictions();

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的中国注册营养师和AI饮食规划师。请根据以下用户信息，生成下一餐的详细饮食建议。\n\n");

        prompt.append("【用户档案】\n");
        prompt.append("- 性别：").append(p.getGender() == 1 ? "男" : "女").append("\n");
        prompt.append("- 年龄：").append(p.getAge()).append("岁\n");
        prompt.append("- 身高：").append(p.getHeight()).append("cm\n");
        prompt.append("- 当前体重：").append(p.getWeight()).append("kg\n");
        prompt.append("- 目标体重：").append(p.getTargetWeight()).append("kg\n");
        prompt.append("- 基础代谢率(BMR)：").append(String.format("%.0f", p.getBmr())).append("kcal\n");
        prompt.append("- 健康目标：").append(
                p.getGoalType() == 1 ? "减脂（需要热量赤字）" :
                        p.getGoalType() == 2 ? "增肌（需要高蛋白和适量 surplus）" : "保持体重（维持当前热量）"
        ).append("\n");
        prompt.append("- 活动水平：").append(p.getActivityLevel()).append("/5（1=久坐，5=极重度）\n\n");

        prompt.append("【饮食限制】\n");
        if ("vegetarian".equals(r.getDietPreference())) {
            prompt.append("- ⚠️ 严格素食：不能推荐任何肉类、鱼类、海鲜、蛋奶。推荐豆制品、坚果、全谷物。\n");
        } else if ("halal".equals(r.getDietPreference())) {
            prompt.append("- ⚠️ 清真饮食：不能推荐猪肉、猪油、动物血液制品。牛羊肉需清真屠宰。\n");
        } else if ("taboo".equals(r.getDietPreference()) && r.getTabooDetail() != null && !r.getTabooDetail().isEmpty()) {
            prompt.append("- ⚠️ 忌口：").append(r.getTabooDetail()).append("\n");
        } else {
            prompt.append("- 无特殊饮食限制\n");
        }
        if (r.getAllergies() != null && !r.getAllergies().isEmpty()) {
            prompt.append("- ⚠️ 过敏源（绝对禁止）：").append(String.join("、", r.getAllergies())).append("\n");
        }
        prompt.append("\n");

        prompt.append("【今日热量状态 - 这是最重要的决策依据】\n");
        prompt.append("- 每日热量目标：").append(c.getTarget()).append("kcal\n");
        prompt.append("- 已摄入热量：").append(c.getConsumed()).append("kcal\n");
        prompt.append("- 剩余可用热量：").append(c.getRemaining()).append("kcal\n");
        prompt.append("- 完成进度：").append(String.format("%.1f", c.getProgressPercent())).append("%\n\n");

        prompt.append("【已记录餐次】\n");
        if (req.getRecordedMeals() != null && !req.getRecordedMeals().isEmpty()) {
            for (DietPlanRequest.RecordedMeal meal : req.getRecordedMeals()) {
                prompt.append("- ").append(meal.getTypeName())
                        .append("：").append(meal.getFoods())
                        .append("（约").append(meal.getCalories()).append("kcal）\n");
            }
        } else {
            prompt.append("今日尚未记录任何餐次\n");
        }
        prompt.append("\n");

        prompt.append("【需要规划的餐次】\n");
        prompt.append("→ ").append(req.getTargetMeal().getName()).append("\n\n");

        prompt.append("【核心约束 - 必须严格遵守】\n");
        int remaining = c.getRemaining();
        if (remaining < 0) {
            prompt.append("🔴 今日热量已超标 ").append(Math.abs(remaining)).append("kcal！\n");
            prompt.append("   → 本餐必须控制在 150-200kcal 以内\n");
            prompt.append("   → 优先推荐：绿叶蔬菜、低糖水果、清汤\n");
            prompt.append("   → 严禁：主食、油脂、高糖食物\n");
        } else if (remaining < 300) {
            prompt.append("🟡 今日热量仅剩 ").append(remaining).append("kcal，非常紧张！\n");
            prompt.append("   → 本餐控制在 200-300kcal\n");
            prompt.append("   → 推荐：蔬菜沙拉、无糖酸奶、少量水果\n");
        } else if (remaining < 600) {
            prompt.append("🟠 今日热量剩余 ").append(remaining).append("kcal，需要精打细算\n");
            prompt.append("   → 本餐控制在 400-500kcal\n");
        } else {
            prompt.append("🟢 今日热量剩余 ").append(remaining).append("kcal，空间充足\n");
            prompt.append("   → 按正常份量规划\n");
        }

        if (p.getGoalType() == 1) {
            prompt.append("- 减脂策略：控制碳水在40-50%，提高蛋白质到25-30%，脂肪20-25%\n");
            prompt.append("- 优先选择：低GI主食、瘦肉、高纤维蔬菜\n");
        } else if (p.getGoalType() == 2) {
            prompt.append("- 增肌策略：碳水50-55%，蛋白质25-30%，脂肪20-25%\n");
            prompt.append("- 优先选择：优质蛋白、复合碳水\n");
        } else {
            prompt.append("- 保持策略：碳水50-55%，蛋白质15-20%，脂肪25-30%\n");
        }

        String mealName = req.getTargetMeal().getName();
        if ("早餐".equals(mealName)) {
            prompt.append("- 早餐原则：必吃！唤醒代谢，碳水+蛋白质为主\n");
        } else if ("午餐".equals(mealName)) {
            prompt.append("- 午餐原则：承上启下，热量最高的一餐\n");
        } else if ("晚餐".equals(mealName)) {
            prompt.append("- 晚餐原则：清淡少油，睡前3小时完成\n");
        } else if ("加餐".equals(mealName)) {
            prompt.append("- 加餐原则：填补饥饿，防止暴食，控制在100-150kcal\n");
        }
        prompt.append("\n");

        prompt.append("【输出要求 - 必须严格按以下JSON格式返回】\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"mealType\": \"餐次名称\",\n");
        prompt.append("  \"totalCalories\": 整数,\n");
        prompt.append("  \"foods\": [\n");
        prompt.append("    {\"name\": \"具体食物名称\", \"amount\": \"具体份量\", \"calories\": 整数, \"image\": \"\"}\n");
        prompt.append("  ],\n");
        prompt.append("  \"nutrition\": {\"carbs\": 整数(0-100), \"protein\": 整数(0-100), \"fat\": 整数(0-100)},\n");
        prompt.append("  \"reason\": \"推荐理由\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("1. 食物必须是中式家常菜，名称具体\n");
        prompt.append("2. 份量具体可操作（如'1碗约200g'）\n");
        prompt.append("3. 总热量与foods各项之和一致\n");
        prompt.append("4. 严格遵守饮食限制和过敏源\n");
        prompt.append("5. 只返回JSON，不要有其他文字");

        return prompt.toString();
    }

    private DietPlanResult parseDietPlanResponse(String response, DietPlanRequest request) {
        try {
            String content = extractContentFromResponse(response);
            System.out.println("AI返回内容: " + content);

            String jsonStr = extractJSONFromContent(content);
            System.out.println("提取后的JSON: " + jsonStr);

            JSONObject resultJson = JSONObject.parseObject(jsonStr);

            DietPlanResult result = new DietPlanResult();
            result.setMealType(resultJson.getString("mealType"));
            result.setTotalCalories(resultJson.getInteger("totalCalories"));
            result.setReason(resultJson.getString("reason"));

            JSONArray foodsArray = resultJson.getJSONArray("foods");
            List<DietPlanResult.FoodItem> foods = new ArrayList<>();
            if (foodsArray != null) {
                for (int i = 0; i < foodsArray.size(); i++) {
                    JSONObject foodJson = foodsArray.getJSONObject(i);
                    DietPlanResult.FoodItem food = new DietPlanResult.FoodItem();
                    food.setName(foodJson.getString("name"));
                    food.setAmount(foodJson.getString("amount"));
                    food.setCalories(foodJson.getInteger("calories"));
                    food.setImage(foodJson.getString("image"));
                    foods.add(food);
                }
            }
            result.setFoods(foods);

            JSONObject nutritionJson = resultJson.getJSONObject("nutrition");
            DietPlanResult.Nutrition nutrition = new DietPlanResult.Nutrition();
            if (nutritionJson != null) {
                nutrition.setCarbs(nutritionJson.getInteger("carbs"));
                nutrition.setProtein(nutritionJson.getInteger("protein"));
                nutrition.setFat(nutritionJson.getInteger("fat"));
            } else {
                nutrition.setCarbs(50);
                nutrition.setProtein(25);
                nutrition.setFat(25);
            }
            result.setNutrition(nutrition);

            int calcTotal = 0;
            for (DietPlanResult.FoodItem f : foods) {
                calcTotal += (f.getCalories() != null ? f.getCalories() : 0);
            }
            if (result.getTotalCalories() == null || Math.abs(result.getTotalCalories() - calcTotal) > 50) {
                result.setTotalCalories(calcTotal);
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("解析规划响应失败: " + e.getMessage());
            return generateFallbackPlan(request);
        }
    }

    private DietPlanResult generateFallbackPlan(DietPlanRequest request) {
        DietPlanRequest.UserProfile p = request.getProfile();
        DietPlanRequest.DietRestrictions r = request.getRestrictions();
        DietPlanRequest.CalorieStatus c = request.getCalorieStatus();
        String mealName = request.getTargetMeal().getName();
        int remaining = c.getRemaining();

        DietPlanResult result = new DietPlanResult();
        result.setMealType(mealName);

        List<DietPlanResult.FoodItem> foods = new ArrayList<>();

        int targetMealCal;
        if (remaining < 0) {
            targetMealCal = 150;
        } else if (remaining < 300) {
            targetMealCal = 250;
        } else if ("早餐".equals(mealName)) {
            targetMealCal = Math.min(500, (int) (remaining * 0.35));
        } else if ("午餐".equals(mealName)) {
            targetMealCal = Math.min(700, (int) (remaining * 0.4));
        } else if ("晚餐".equals(mealName)) {
            targetMealCal = Math.min(500, (int) (remaining * 0.25));
        } else {
            targetMealCal = 150;
        }

        boolean isVegetarian = "vegetarian".equals(r.getDietPreference());
        boolean isHalal = "halal".equals(r.getDietPreference());

        if ("早餐".equals(mealName)) {
            foods.add(createPlanFood("全麦面包", "2片", 120));
            foods.add(createPlanFood(isVegetarian ? "豆浆" : "牛奶", "1杯250ml", isVegetarian ? 80 : 130));
            foods.add(createPlanFood("水煮蛋", "1个", 70));
            if (!isVegetarian) foods.add(createPlanFood("小番茄", "5颗", 30));
        } else if ("午餐".equals(mealName)) {
            foods.add(createPlanFood("糙米饭", "1碗200g", 200));
            if (isVegetarian) {
                foods.add(createPlanFood("麻婆豆腐", "150g", 150));
            } else if (isHalal) {
                foods.add(createPlanFood("清炖牛肉", "100g", 180));
            } else {
                foods.add(createPlanFood("清蒸鸡胸肉", "100g", 133));
            }
            foods.add(createPlanFood("蒜蓉西兰花", "150g", 80));
            foods.add(createPlanFood("紫菜蛋花汤", "1碗", 50));
        } else if ("晚餐".equals(mealName)) {
            foods.add(createPlanFood("杂粮粥", "1碗", 120));
            if (isVegetarian) {
                foods.add(createPlanFood("凉拌黄瓜豆腐丝", "150g", 100));
            } else {
                foods.add(createPlanFood("清蒸鱼", "100g", 120));
            }
            foods.add(createPlanFood("白灼菜心", "150g", 50));
        } else {
            foods.add(createPlanFood("苹果", "1个", 80));
            foods.add(createPlanFood("无糖酸奶", "1杯", 70));
        }

        result.setFoods(foods);
        int total = 0;
        for (DietPlanResult.FoodItem f : foods) {
            total += (f.getCalories() != null ? f.getCalories() : 0);
        }
        result.setTotalCalories(total);

        DietPlanResult.Nutrition nutrition = new DietPlanResult.Nutrition();
        nutrition.setCarbs(50);
        nutrition.setProtein(25);
        nutrition.setFat(25);
        result.setNutrition(nutrition);

        String goalText = p.getGoalType() == 1 ? "减脂" : p.getGoalType() == 2 ? "增肌" : "保持";
        result.setReason("AI 服务繁忙，使用默认推荐方案。基于您的" + goalText + "目标和今日剩余 " + remaining + "kcal 生成。" +
                (remaining < 0 ? "注意：今日已超标，本餐已尽量控制热量。" : ""));

        return result;
    }

    private DietPlanResult.FoodItem createPlanFood(String name, String amount, int calories) {
        DietPlanResult.FoodItem food = new DietPlanResult.FoodItem();
        food.setName(name);
        food.setAmount(amount);
        food.setCalories(calories);
        food.setImage("");
        return food;
    }

    // ==================== 全天饮食规划 ====================

    public DailyPlanResult generateDailyPlan(DietPlanRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            System.out.println("========== 全天饮食规划 ==========");
            System.out.println("用户ID: " + request.getUserId());

            String prompt = buildDailyPlanPrompt(request);

            Map<String, Object> requestBody = buildTextRequestBody(prompt, 3500);

            String response = sendHttpRequest(requestBody);
            System.out.println("API返回: " + response);

            DailyPlanResult result = parseDailyPlanResponse(response, request);

            long processTime = System.currentTimeMillis() - startTime;
            saveAiLog(request.getUserId(), null, null, "daily-plan",
                    JSON.toJSONString(result), processTime);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("全天规划AI失败，使用降级方案");
            return generateDailyFallback(request);
        }
    }

    private String buildDailyPlanPrompt(DietPlanRequest req) {
        DietPlanRequest.UserProfile p = req.getProfile();
        DietPlanRequest.CalorieStatus c = req.getCalorieStatus();
        DietPlanRequest.DietRestrictions r = req.getRestrictions();

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的中国注册营养师。请为用户规划今日全部餐次的饮食方案。\n\n");

        prompt.append("【用户档案】\n");
        prompt.append("- 性别：").append(p.getGender() == 1 ? "男" : "女").append("\n");
        prompt.append("- 年龄：").append(p.getAge()).append("岁\n");
        prompt.append("- 身高：").append(p.getHeight()).append("cm\n");
        prompt.append("- 当前体重：").append(p.getWeight()).append("kg\n");
        prompt.append("- 目标体重：").append(p.getTargetWeight()).append("kg\n");
        prompt.append("- BMR：").append(String.format("%.0f", p.getBmr())).append("kcal\n");
        prompt.append("- 健康目标：").append(
                p.getGoalType() == 1 ? "减脂" :
                        p.getGoalType() == 2 ? "增肌" : "保持体重"
        ).append("\n");
        prompt.append("- 活动水平：").append(p.getActivityLevel()).append("/5\n\n");

        prompt.append("【饮食限制】\n");
        if ("vegetarian".equals(r.getDietPreference())) {
            prompt.append("- 严格素食：禁止肉类、鱼类、海鲜。推荐豆制品、全谷物\n");
        } else if ("halal".equals(r.getDietPreference())) {
            prompt.append("- 清真饮食：禁止猪肉、猪油。牛羊肉需清真\n");
        } else if ("taboo".equals(r.getDietPreference()) && r.getTabooDetail() != null && !r.getTabooDetail().isEmpty()) {
            prompt.append("- 忌口：").append(r.getTabooDetail()).append("\n");
        } else {
            prompt.append("- 无特殊限制\n");
        }
        if (r.getAllergies() != null && !r.getAllergies().isEmpty()) {
            prompt.append("- 过敏源（绝对禁止）：").append(String.join("、", r.getAllergies())).append("\n");
        }
        prompt.append("\n");

        prompt.append("【每日热量目标】\n");
        prompt.append("- 目标：").append(c.getTarget()).append("kcal\n\n");

        prompt.append("【已记录餐次 - 不要再规划这些】\n");
        if (req.getRecordedMeals() != null && !req.getRecordedMeals().isEmpty()) {
            for (DietPlanRequest.RecordedMeal meal : req.getRecordedMeals()) {
                prompt.append("- ").append(meal.getTypeName())
                        .append("：").append(meal.getFoods())
                        .append("（约").append(meal.getCalories()).append("kcal）\n");
            }
        } else {
            prompt.append("今日尚未记录任何餐次\n");
        }
        prompt.append("\n");

        prompt.append("【需要规划的餐次】\n");
        if (req.getTargetMeals() != null && !req.getTargetMeals().isEmpty()) {
            for (DietPlanRequest.TargetMeal meal : req.getTargetMeals()) {
                prompt.append("- ").append(meal.getName()).append("\n");
            }
        } else if (req.getTargetMeal() != null) {
            prompt.append("- ").append(req.getTargetMeal().getName()).append("\n");
        } else {
            prompt.append("- 早餐\n- 午餐\n- 晚餐\n- 加餐\n");
        }
        prompt.append("\n");

        int remaining = c.getRemaining();
        prompt.append("【热量分配策略】\n");
        if (remaining <= 0) {
            prompt.append("今日热量已超标 ").append(Math.abs(remaining)).append("kcal！\n");
            prompt.append("所有未记录餐次合计控制在 200kcal 以内，以蔬菜、清汤为主\n");
        } else {
            prompt.append("剩余 ").append(remaining).append("kcal 需分配到未记录餐次\n");
            prompt.append("参考比例：早餐30% / 午餐35% / 晚餐25% / 加餐10%\n");
        }

        if (p.getGoalType() == 1) {
            prompt.append("减脂策略：碳水40-45%，蛋白质30-35%，脂肪20-25%\n");
        } else if (p.getGoalType() == 2) {
            prompt.append("增肌策略：碳水45-50%，蛋白质30%，脂肪20-25%\n");
        } else {
            prompt.append("保持策略：碳水50%，蛋白质20%，脂肪30%\n");
        }
        prompt.append("\n");

        prompt.append("【输出要求 - 严格按此JSON格式返回】\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"meals\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"mealType\": \"早餐\",\n");
        prompt.append("      \"mealTypeCode\": 1,\n");
        prompt.append("      \"totalCalories\": 400,\n");
        prompt.append("      \"foods\": [\n");
        prompt.append("        {\"name\": \"燕麦粥\", \"amount\": \"1碗(200g)\", \"calories\": 150, \"carbs\": 27, \"protein\": 5, \"fat\": 3}\n");
        prompt.append("      ],\n");
        prompt.append("      \"nutrition\": {\"carbs\": 48, \"protein\": 28, \"fat\": 24},\n");
        prompt.append("      \"reason\": \"推荐理由\"\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"dailySummary\": {\n");
        prompt.append("    \"totalCalories\": 1500,\n");
        prompt.append("    \"totalCarbs\": 170,\n");
        prompt.append("    \"totalProtein\": 110,\n");
        prompt.append("    \"totalFat\": 45,\n");
        prompt.append("    \"advice\": \"今日整体饮食建议\"\n");
        prompt.append("  }\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("【规则】\n");
        prompt.append("1. 只规划上面列出的餐次，已记录的不要重复\n");
        prompt.append("2. 食物必须是中式家常菜，名称具体\n");
        prompt.append("3. 份量具体可操作（如'1碗约200g'）\n");
        prompt.append("4. carbs/protein/fat 是该食物的克数\n");
        prompt.append("5. nutrition 里的 carbs/protein/fat 是百分比，三者之和为100\n");
        prompt.append("6. 各餐 totalCalories 之和 = dailySummary.totalCalories\n");
        prompt.append("7. 严格遵守饮食限制和过敏源\n");
        prompt.append("8. mealTypeCode：早餐=1，午餐=2，晚餐=3，加餐=4\n");
        prompt.append("9. 只返回JSON，不要有其他文字");

        return prompt.toString();
    }

    private DailyPlanResult parseDailyPlanResponse(String response, DietPlanRequest request) {
        try {
            String content = extractContentFromResponse(response);
            System.out.println("AI返回内容: " + content);

            String jsonStr = extractJSONFromContent(content);
            System.out.println("提取后的JSON: " + jsonStr);

            JSONObject resultJson = JSONObject.parseObject(jsonStr);
            if (resultJson == null) {
                throw new RuntimeException("JSON解析返回null");
            }

            DailyPlanResult result = new DailyPlanResult();

            JSONArray mealsArray = resultJson.getJSONArray("meals");
            List<DailyPlanResult.MealPlan> meals = new ArrayList<>();

            if (mealsArray != null) {
                for (int i = 0; i < mealsArray.size(); i++) {
                    JSONObject mealJson = mealsArray.getJSONObject(i);
                    DailyPlanResult.MealPlan meal = new DailyPlanResult.MealPlan();

                    meal.setMealType(mealJson.getString("mealType"));
                    meal.setMealTypeCode(mealJson.getInteger("mealTypeCode"));
                    meal.setTotalCalories(mealJson.getInteger("totalCalories"));
                    meal.setReason(mealJson.getString("reason"));
                    meal.setRecorded(false);

                    JSONArray foodsArray = mealJson.getJSONArray("foods");
                    List<DailyPlanResult.FoodItem> foods = new ArrayList<>();
                    if (foodsArray != null) {
                        for (int j = 0; j < foodsArray.size(); j++) {
                            JSONObject foodJson = foodsArray.getJSONObject(j);
                            DailyPlanResult.FoodItem food = new DailyPlanResult.FoodItem();
                            food.setName(foodJson.getString("name"));
                            food.setAmount(foodJson.getString("amount"));
                            food.setCalories(foodJson.getInteger("calories"));
                            food.setCarbs(foodJson.getInteger("carbs"));
                            food.setProtein(foodJson.getInteger("protein"));
                            food.setFat(foodJson.getInteger("fat"));
                            food.setImage("");
                            foods.add(food);
                        }
                    }
                    meal.setFoods(foods);

                    int calcCal = 0;
                    for (DailyPlanResult.FoodItem f : foods) {
                        calcCal += (f.getCalories() != null ? f.getCalories() : 0);
                    }
                    if (meal.getTotalCalories() == null || Math.abs(meal.getTotalCalories() - calcCal) > 50) {
                        meal.setTotalCalories(calcCal);
                    }

                    JSONObject nutJson = mealJson.getJSONObject("nutrition");
                    DailyPlanResult.Nutrition nutrition = new DailyPlanResult.Nutrition();
                    if (nutJson != null) {
                        nutrition.setCarbs(nutJson.getInteger("carbs"));
                        nutrition.setProtein(nutJson.getInteger("protein"));
                        nutrition.setFat(nutJson.getInteger("fat"));
                    } else {
                        nutrition.setCarbs(50);
                        nutrition.setProtein(25);
                        nutrition.setFat(25);
                    }
                    meal.setNutrition(nutrition);

                    meals.add(meal);
                }
            }

            if (request.getRecordedMeals() != null) {
                for (DietPlanRequest.RecordedMeal recorded : request.getRecordedMeals()) {
                    for (DailyPlanResult.MealPlan meal : meals) {
                        if (recorded.getType() != null && recorded.getType().equals(meal.getMealTypeCode())) {
                            meal.setRecorded(true);
                        }
                    }
                }
            }

            result.setMeals(meals);

            JSONObject summaryJson = resultJson.getJSONObject("dailySummary");
            DailyPlanResult.DailySummary summary = new DailyPlanResult.DailySummary();
            if (summaryJson != null) {
                summary.setTotalCalories(summaryJson.getInteger("totalCalories"));
                summary.setTotalCarbs(summaryJson.getInteger("totalCarbs"));
                summary.setTotalProtein(summaryJson.getInteger("totalProtein"));
                summary.setTotalFat(summaryJson.getInteger("totalFat"));
                summary.setAdvice(summaryJson.getString("advice"));
            } else {
                int total = 0;
                for (DailyPlanResult.MealPlan m : meals) {
                    total += (m.getTotalCalories() != null ? m.getTotalCalories() : 0);
                }
                summary.setTotalCalories(total);
            }
            result.setDailySummary(summary);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("解析全天规划失败: " + e.getMessage());
            return generateDailyFallback(request);
        }
    }

    private DailyPlanResult generateDailyFallback(DietPlanRequest request) {
        DietPlanRequest.UserProfile p = request.getProfile();
        DietPlanRequest.DietRestrictions r = request.getRestrictions();
        int remaining = request.getCalorieStatus().getRemaining();
        int consumed = request.getCalorieStatus().getConsumed();

        List<Integer> recordedTypes = new ArrayList<>();
        if (request.getRecordedMeals() != null) {
            for (DietPlanRequest.RecordedMeal m : request.getRecordedMeals()) {
                if (m.getType() != null) {
                    recordedTypes.add(m.getType());
                }
            }
        }

        boolean isVegetarian = "vegetarian".equals(r.getDietPreference());
        boolean isHalal = "halal".equals(r.getDietPreference());

        List<DailyPlanResult.MealPlan> meals = new ArrayList<>();
        int totalPlanned = 0;

        if (!recordedTypes.contains(1)) {
            List<DailyPlanResult.FoodItem> foods = new ArrayList<>();
            foods.add(dailyFood("燕麦粥", "1碗(200g)", 150, 27, 5, 3));
            foods.add(dailyFood("水煮蛋", "2个", 140, 2, 12, 10));
            if (isVegetarian) {
                foods.add(dailyFood("豆浆", "1杯(300ml)", 80, 8, 6, 3));
            } else {
                foods.add(dailyFood("全脂牛奶", "1杯(250ml)", 130, 12, 8, 8));
            }
            foods.add(dailyFood("小番茄", "5颗", 30, 6, 1, 0));
            int cal = sumCal(foods);
            meals.add(dailyMeal("早餐", 1, cal, foods, dailyNutrition(48, 28, 24),
                    "早餐唤醒代谢，以碳水+蛋白质为主，搭配果蔬补充维生素"));
            totalPlanned += cal;
        }

        if (!recordedTypes.contains(2)) {
            List<DailyPlanResult.FoodItem> foods = new ArrayList<>();
            foods.add(dailyFood("糙米饭", "1碗(200g)", 232, 48, 6, 2));
            if (isVegetarian) {
                foods.add(dailyFood("麻婆豆腐", "150g", 150, 8, 12, 9));
            } else if (isHalal) {
                foods.add(dailyFood("清炖牛肉", "100g", 180, 0, 26, 8));
            } else {
                foods.add(dailyFood("清蒸鸡胸肉", "100g", 133, 0, 31, 3));
            }
            foods.add(dailyFood("蒜蓉西兰花", "150g", 80, 10, 6, 3));
            foods.add(dailyFood("紫菜蛋花汤", "1碗", 50, 4, 4, 2));
            int cal = sumCal(foods);
            meals.add(dailyMeal("午餐", 2, cal, foods, dailyNutrition(45, 32, 23),
                    "午餐热量最高，主食+优质蛋白+大量蔬菜，保证下午能量供给"));
            totalPlanned += cal;
        }

        if (!recordedTypes.contains(3)) {
            List<DailyPlanResult.FoodItem> foods = new ArrayList<>();
            foods.add(dailyFood("杂粮粥", "1碗", 120, 24, 4, 1));
            if (isVegetarian) {
                foods.add(dailyFood("凉拌黄瓜豆腐丝", "150g", 100, 8, 8, 5));
            } else {
                foods.add(dailyFood("清蒸鲈鱼", "100g", 105, 0, 20, 2));
            }
            foods.add(dailyFood("白灼菜心", "150g", 45, 6, 3, 1));
            int cal = sumCal(foods);
            meals.add(dailyMeal("晚餐", 3, cal, foods, dailyNutrition(50, 28, 22),
                    "晚餐清淡易消化，少量主食+优质蛋白+蔬菜，睡前3小时完成"));
            totalPlanned += cal;
        }

        if (!recordedTypes.contains(4) && (remaining - totalPlanned) > 100) {
            List<DailyPlanResult.FoodItem> foods = new ArrayList<>();
            foods.add(dailyFood("苹果", "1个(200g)", 80, 18, 0, 0));
            foods.add(dailyFood("无糖酸奶", "1杯(150g)", 70, 8, 5, 2));
            int cal = sumCal(foods);
            meals.add(dailyMeal("加餐", 4, cal, foods, dailyNutrition(60, 20, 20),
                    "加餐补充能量缺口，以水果+乳制品为主"));
            totalPlanned += cal;
        }

        DailyPlanResult result = new DailyPlanResult();
        result.setMeals(meals);

        DailyPlanResult.DailySummary summary = new DailyPlanResult.DailySummary();
        summary.setTotalCalories(totalPlanned + consumed);
        summary.setAdvice("本地推荐方案，基于您的" +
                (p.getGoalType() == 1 ? "减脂" : p.getGoalType() == 2 ? "增肌" : "保持") +
                "目标生成。");
        result.setDailySummary(summary);

        return result;
    }

    // ==================== 运动规划 ====================

    public com.eatnotfat.backend.vo.ExerciseDashboardVO.ExercisePlanVO generateExercisePlan(
            com.eatnotfat.backend.dto.ExercisePlanRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            System.out.println("========== AI运动规划 ==========");
            System.out.println("用户ID: " + request.getUserId());

            String prompt = buildExercisePlanPrompt(request);
            Map<String, Object> requestBody = buildTextRequestBody(prompt, 2000);
            String response = sendHttpRequest(requestBody);

            com.eatnotfat.backend.vo.ExerciseDashboardVO.ExercisePlanVO result =
                    parseExercisePlanResponse(response);

            long processTime = System.currentTimeMillis() - startTime;
            saveAiLog(request.getUserId(), null, null, "exercise-plan",
                    JSON.toJSONString(result), processTime);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("AI运动规划失败，使用降级方案");
            return generateExerciseFallback(request);
        }
    }

    private String buildExercisePlanPrompt(ExercisePlanRequest req) {
        ExercisePlanRequest.UserProfile p = req.getProfile();
        ExercisePlanRequest.WeeklyDietData diet = req.getWeeklyDiet();
        ExercisePlanRequest.WeeklyExerciseData ex = req.getWeeklyExercise();
        ExercisePlanRequest.FatigueData fatigue = req.getFatigue();

        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的运动健康规划师，请根据以下用户完整数据，灵活生成今日个性化运动计划。\n\n");

        sb.append("【用户档案】\n");
        sb.append("- 性别：").append(p.getGenderLabel()).append("\n");
        sb.append("- 年龄：").append(p.getAge()).append("岁\n");
        sb.append("- 身高：").append(p.getHeight()).append("cm\n");
        sb.append("- 当前体重：").append(p.getWeight()).append("kg\n");
        sb.append("- 目标体重：").append(p.getTargetWeight()).append("kg\n");
        sb.append("- 体重差值：");
        if (p.getWeightDiff() > 0) sb.append("需减重").append(p.getWeightDiff()).append("kg");
        else if (p.getWeightDiff() < 0) sb.append("需增重").append(Math.abs(p.getWeightDiff())).append("kg");
        else sb.append("已达标");
        sb.append("\n");
        sb.append("- 日常活动水平：").append(p.getActivityLabel()).append("\n");
        sb.append("- 健康目标：").append(p.getGoalLabel()).append("\n");
        sb.append("- 运动经验：").append(p.getExerciseLevelLabel()).append("\n");
        sb.append("- 基础代谢(BMR)：").append(String.format("%.0f", p.getBmr())).append("kcal\n");
        sb.append("- 每日总消耗(TDEE)：").append(String.format("%.0f", p.getTdee())).append("kcal\n");
        sb.append("- 每日目标摄入：").append(String.format("%.0f", p.getTargetCalorie())).append("kcal\n");
        sb.append("- 建议运动消耗：").append(String.format("%.0f", p.getExerciseCalorieGoal())).append("kcal\n\n");

        sb.append("【近7日饮食数据】\n");
        sb.append("- 日均摄入：").append(diet.getAvgCalories()).append("kcal\n");
        sb.append("- 目标摄入：").append(diet.getTargetCalories()).append("kcal\n");
        sb.append("- 超标天数：").append(diet.getSurplusDays()).append("天\n");
        sb.append("- 累计盈余：").append(diet.getTotalSurplus()).append("kcal\n\n");

        sb.append("【近7日运动数据】\n");
        sb.append("- 运动天数：").append(ex.getActiveDays()).append("天\n");
        sb.append("- 总消耗：").append(ex.getTotalBurned()).append("kcal\n");
        sb.append("- 平均时长：").append(ex.getAvgDuration()).append("分钟\n");
        sb.append("- 最近运动：").append(
                ex.getRecentTypes() != null && !ex.getRecentTypes().isEmpty()
                        ? String.join("、", ex.getRecentTypes()) : "无"
        ).append("\n\n");

        sb.append("【疲劳度】\n");
        sb.append("- 连续运动：").append(fatigue.getConsecutiveDays()).append("天\n");
        sb.append("- 建议休息：").append(fatigue.isNeedRest() ? "是" : "否").append("\n\n");

        sb.append("【要求】\n");
        sb.append("1. 综合考虑用户年龄、性别、体重、目标、活动水平、运动经验和疲劳度，灵活生成最适合的运动计划\n");
        sb.append("2. 运动强度、时长、类型必须与用户身体条件匹配，不得安排超出用户承受能力的运动\n");
        sb.append("3. 如果用户年龄较大或体重基数大，避免高冲击、高负重动作，优先保护关节\n");
        sb.append("4. 如果近期饮食超标，适当增加运动量；如果疲劳度高，降低强度或建议休息\n");
        sb.append("5. 每个运动都要给出推荐理由，结合用户具体数据说明为什么适合\n\n");

        sb.append("【输出格式】严格按JSON输出，不要其他内容：\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"exercises\": [\n");
        sb.append("    {\n");
        sb.append("      \"name\": \"运动名称\",\n");
        sb.append("      \"icon\": \"running/squat/yoga等\",\n");
        sb.append("      \"category\": \"cardio/strength/flexibility/hiit\",\n");
        sb.append("      \"duration\": 分钟数,\n");
        sb.append("      \"calories\": 预估消耗kcal,\n");
        sb.append("      \"intensity\": 1低/2中/3高,\n");
        sb.append("      \"reason\": \"推荐理由（引用用户具体数据）\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"totalDuration\": 总时长,\n");
        sb.append("  \"totalCalories\": 总消耗,\n");
        sb.append("  \"advice\": \"个性化建议（100字以内）\"\n");
        sb.append("}\n");
        sb.append("```\n");

        return sb.toString();
    }

    private com.eatnotfat.backend.vo.ExerciseDashboardVO.ExercisePlanVO parseExercisePlanResponse(String response) {
        com.eatnotfat.backend.vo.ExerciseDashboardVO.ExercisePlanVO plan =
                new com.eatnotfat.backend.vo.ExerciseDashboardVO.ExercisePlanVO();

        try {
            String content = extractContentFromResponse(response);
            String jsonStr = extractJSONFromContent(content);

            JSONObject root = JSONObject.parseObject(jsonStr);
            JSONArray exercisesArr = root.getJSONArray("exercises");

            List<com.eatnotfat.backend.vo.ExerciseDashboardVO.PlanExercise> exercises = new ArrayList<>();
            if (exercisesArr != null) {
                for (int i = 0; i < exercisesArr.size(); i++) {
                    JSONObject ex = exercisesArr.getJSONObject(i);
                    com.eatnotfat.backend.vo.ExerciseDashboardVO.PlanExercise pe =
                            new com.eatnotfat.backend.vo.ExerciseDashboardVO.PlanExercise();
                    pe.setName(ex.getString("name"));
                    pe.setIcon(ex.getString("icon"));
                    pe.setCategory(ex.getString("category"));
                    pe.setDuration(ex.getIntValue("duration"));
                    pe.setCalories(ex.getIntValue("calories"));
                    pe.setIntensity(ex.getString("intensity"));
                    pe.setReason(ex.getString("reason"));
                    exercises.add(pe);
                }
            }
            plan.setExercises(exercises);
            plan.setTotalDuration(root.getIntValue("totalDuration"));
            plan.setTotalCalories(root.getIntValue("totalCalories"));
            plan.setAdvice(root.getString("advice"));
            plan.setGenerated(true);
        } catch (Exception e) {
            e.printStackTrace();
            plan.setGenerated(false);
            plan.setExercises(new ArrayList<>());
        }

        return plan;
    }

    private com.eatnotfat.backend.vo.ExerciseDashboardVO.ExercisePlanVO generateExerciseFallback(
            com.eatnotfat.backend.dto.ExercisePlanRequest request) {
        com.eatnotfat.backend.vo.ExerciseDashboardVO.ExercisePlanVO plan =
                new com.eatnotfat.backend.vo.ExerciseDashboardVO.ExercisePlanVO();

        ExercisePlanRequest.UserProfile p = request.getProfile();
        ExercisePlanRequest.FatigueData fatigue = request.getFatigue();
        List<com.eatnotfat.backend.vo.ExerciseDashboardVO.PlanExercise> exercises = new ArrayList<>();

        if (fatigue.isNeedRest()) {
            exercises.add(planExercise("拉伸放松", "stretch", "flexibility", 15, 50, "低", "连续运动多日，拉伸促进恢复"));
            exercises.add(planExercise("散步", "walking", "cardio", 20, 80, "低", "轻度活动促进血液循环"));
        } else if (p.getGoalType() == 1) {
            exercises.add(planExercise("慢跑", "running", "cardio", 30, 250, "中", "有氧燃脂，保持心率在最大心率60-70%"));
            exercises.add(planExercise("波比跳", "burpee", "hiit", 10, 120, "高", "HIIT高效燃脂，提升代谢"));
            exercises.add(planExercise("拉伸", "stretch", "flexibility", 10, 30, "低", "运动后拉伸防止受伤"));
        } else if (p.getGoalType() == 2) {
            exercises.add(planExercise("深蹲", "squat", "strength", 15, 80, "中", "锻炼腿部和臀部肌群"));
            exercises.add(planExercise("俯卧撑", "push-up", "strength", 15, 70, "中", "锻炼胸肌和手臂"));
            exercises.add(planExercise("平板支撑", "push-up", "strength", 10, 40, "中", "核心力量训练"));
        } else {
            exercises.add(planExercise("快走", "walking", "cardio", 30, 150, "低", "低冲击有氧，适合日常"));
            exercises.add(planExercise("瑜伽", "yoga", "flexibility", 20, 80, "低", "提升柔韧性和放松身心"));
        }

        int totalDuration = 0;
        int totalCalories = 0;
        for (com.eatnotfat.backend.vo.ExerciseDashboardVO.PlanExercise e : exercises) {
            totalDuration += e.getDuration();
            totalCalories += e.getCalories();
        }

        plan.setExercises(exercises);
        plan.setTotalDuration(totalDuration);
        plan.setTotalCalories(totalCalories);
        String goalText = p.getGoalType() == 1 ? "减脂" : p.getGoalType() == 2 ? "增肌" : "保持";
        plan.setAdvice("AI服务繁忙，使用默认推荐方案。基于您的" + goalText + "目标生成。");
        plan.setGenerated(true);

        return plan;
    }

    private com.eatnotfat.backend.vo.ExerciseDashboardVO.PlanExercise planExercise(
            String name, String icon, String category, int duration, int calories, String intensity, String reason) {
        com.eatnotfat.backend.vo.ExerciseDashboardVO.PlanExercise pe =
                new com.eatnotfat.backend.vo.ExerciseDashboardVO.PlanExercise();
        pe.setName(name);
        pe.setIcon(icon);
        pe.setCategory(category);
        pe.setDuration(duration);
        pe.setCalories(calories);
        pe.setIntensity(intensity);
        pe.setReason(reason);
        return pe;
    }

    // ==================== 公共方法 ====================

    private Map<String, Object> buildTextRequestBody(String prompt, int maxTokens) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", qwenConfig.getModel());

        Map<String, Object> input = new HashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一位专业的中国注册营养师，擅长根据用户的身体状况、健康目标和饮食记录，制定个性化的饮食方案。你的建议必须科学、实用、符合中式饮食习惯。只返回JSON，不要有其他文字。");
        messages.add(systemMessage);

        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        input.put("messages", messages);
        request.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("result_format", "message");
        parameters.put("max_tokens", maxTokens);
        request.put("parameters", parameters);

        return request;
    }

    private String extractContentFromResponse(String response) {
        try {
            JSONObject json = JSONObject.parseObject(response);
            JSONObject output = json.getJSONObject("output");
            if (output == null) return response;

            JSONArray choices = output.getJSONArray("choices");
            if (choices == null || choices.size() == 0) return response;

            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");

            // 兼容多模态返回格式（content可能是数组）
            Object contentObj = message.get("content");
            if (contentObj instanceof JSONArray) {
                JSONArray contentArray = (JSONArray) contentObj;
                for (int i = 0; i < contentArray.size(); i++) {
                    JSONObject item = contentArray.getJSONObject(i);
                    if (item.containsKey("text")) {
                        return item.getString("text");
                    }
                }
                return contentArray.toJSONString();
            }
            return message.getString("content");
        } catch (Exception e) {
            return response;
        }
    }

    private String extractJSONFromContent(String content) {
        String text = content;

        if (content.startsWith("[") && content.endsWith("]")) {
            try {
                JSONArray array = JSONArray.parseArray(content);
                if (array != null && array.size() > 0) {
                    JSONObject first = array.getJSONObject(0);
                    if (first.containsKey("text")) {
                        text = first.getString("text");
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (text.contains("```json")) {
            int start = text.indexOf("```json") + 7;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }
        if (text.contains("```")) {
            int start = text.indexOf("```") + 3;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }

        return text.trim();
    }

    // ==================== 全天规划辅助方法 ====================

    private DailyPlanResult.FoodItem dailyFood(String name, String amount, int cal, int carbs, int protein, int fat) {
        DailyPlanResult.FoodItem food = new DailyPlanResult.FoodItem();
        food.setName(name);
        food.setAmount(amount);
        food.setCalories(cal);
        food.setCarbs(carbs);
        food.setProtein(protein);
        food.setFat(fat);
        food.setImage("");
        return food;
    }

    private DailyPlanResult.Nutrition dailyNutrition(int carbs, int protein, int fat) {
        DailyPlanResult.Nutrition n = new DailyPlanResult.Nutrition();
        n.setCarbs(carbs);
        n.setProtein(protein);
        n.setFat(fat);
        return n;
    }

    private DailyPlanResult.MealPlan dailyMeal(String mealType, int code, int cal,
                                               List<DailyPlanResult.FoodItem> foods,
                                               DailyPlanResult.Nutrition nutrition, String reason) {
        DailyPlanResult.MealPlan meal = new DailyPlanResult.MealPlan();
        meal.setMealType(mealType);
        meal.setMealTypeCode(code);
        meal.setTotalCalories(cal);
        meal.setFoods(foods);
        meal.setNutrition(nutrition);
        meal.setReason(reason);
        meal.setRecorded(false);
        return meal;
    }

    private int sumCal(List<DailyPlanResult.FoodItem> foods) {
        int sum = 0;
        for (DailyPlanResult.FoodItem f : foods) {
            sum += (f.getCalories() != null ? f.getCalories() : 0);
        }
        return sum;
    }

    // ==================== 食物识别相关 ====================

    private Map<String, Object> buildRecognizeRequestBody(String imageUrl, String imageBase64) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", qwenConfig.getVlModel());

        Map<String, Object> input = new HashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");

        List<Map<String, Object>> contents = new ArrayList<>();

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("text", buildRecognizePrompt());
        contents.add(textContent);

        Map<String, Object> imageContent = new HashMap<>();
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            imageContent.put("image", imageBase64);
            System.out.println("使用 Base64 图片，长度: " + imageBase64.length());
        } else if (imageUrl != null && !imageUrl.isEmpty()) {
            imageContent.put("image", imageUrl);
            System.out.println("使用 URL 图片: " + imageUrl);
        } else {
            throw new RuntimeException("没有提供图片");
        }
        contents.add(imageContent);

        message.put("content", contents);
        messages.add(message);

        input.put("messages", messages);
        request.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("result_format", "message");
        request.put("parameters", parameters);

        return request;
    }

    private String buildRecognizePrompt() {
        return "请识别图片中的食物，并返回JSON格式的结果。"
                + "要求：\n"
                + "1. 如果有多样食物，都列出来\n"
                + "2. 估算每样食物的重量（克）\n"
                + "3. 提供每100g的热量（kcal）、碳水化合物（g）、蛋白质（g）、脂肪（g）\n"
                + "4. 给出识别置信度（0-1之间）\n\n"
                + "返回格式示例：\n"
                + "{\n"
                + "  \"foods\": [\n"
                + "    {\n"
                + "      \"name\": \"米饭\",\n"
                + "      \"calorie\": 116,\n"
                + "      \"carbs\": 25.6,\n"
                + "      \"protein\": 2.6,\n"
                + "      \"fat\": 0.3,\n"
                + "      \"confidence\": 0.95,\n"
                + "      \"estimated_weight\": 150\n"
                + "    }\n"
                + "  ]\n"
                + "}\n"
                + "只返回JSON，不要有其他文字。";
    }

    private RecognizeResult parseRecognizeResponse(String response) {
        RecognizeResult result = new RecognizeResult();
        List<RecognizeResult.FoodItem> foods = new ArrayList<>();

        try {
            String content = extractContentFromResponse(response);
            System.out.println("AI返回内容: " + content);

            String jsonStr = extractJSONFromContent(content);
            System.out.println("提取后的JSON: " + jsonStr);

            JSONObject resultJson = JSONObject.parseObject(jsonStr);
            JSONArray foodsArray = resultJson.getJSONArray("foods");

            if (foodsArray != null) {
                for (int i = 0; i < foodsArray.size(); i++) {
                    JSONObject foodJson = foodsArray.getJSONObject(i);

                    RecognizeResult.FoodItem food = new RecognizeResult.FoodItem();
                    food.setName(foodJson.getString("name"));
                    food.setCalorie(foodJson.getBigDecimal("calorie"));
                    food.setCarbs(foodJson.getBigDecimal("carbs"));
                    food.setProtein(foodJson.getBigDecimal("protein"));
                    food.setFat(foodJson.getBigDecimal("fat"));
                    food.setConfidence(foodJson.getDouble("confidence"));
                    food.setWeight(foodJson.getBigDecimal("estimated_weight"));

                    if (food.getWeight() == null) {
                        food.setWeight(new BigDecimal("100"));
                    }

                    foods.add(food);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("解析响应失败: " + e.getMessage());
            return getMockResult();
        }

        double avgConfidence = 0;
        int count = 0;
        for (RecognizeResult.FoodItem f : foods) {
            if (f.getConfidence() != null) {
                avgConfidence += f.getConfidence();
                count++;
            }
        }
        avgConfidence = count > 0 ? avgConfidence / count : 0.85;

        result.setFoods(foods);
        result.setConfidence(avgConfidence);

        return result;
    }

    private void matchLocalFoods(RecognizeResult result) {
        List<FoodStandard> foodList = foodStandardMapper.selectAllEnabled();
        System.out.println("加载食物库数量: " + foodList.size());

        for (RecognizeResult.FoodItem food : result.getFoods()) {
            String foodName = food.getName();
            Long matchedFoodId = findMatchedFoodId(foodName, foodList);
            food.setFoodId(matchedFoodId);
            System.out.println("食物 '" + foodName + "' 匹配到ID: " + matchedFoodId);
        }
    }

    private Long findMatchedFoodId(String foodName, List<FoodStandard> foodList) {
        if (foodName == null || foodName.isEmpty()) {
            return 0L;
        }

        for (FoodStandard food : foodList) {
            if (food.getName().equals(foodName)) {
                return food.getId();
            }
        }

        for (FoodStandard food : foodList) {
            if (food.getName().contains(foodName) || foodName.contains(food.getName())) {
                return food.getId();
            }
        }

        for (FoodStandard food : foodList) {
            if (food.getAlias() != null && !food.getAlias().isEmpty()) {
                String[] aliases = food.getAlias().split(",");
                for (String alias : aliases) {
                    String trimmedAlias = alias.trim();
                    if (trimmedAlias.equals(foodName)
                            || trimmedAlias.contains(foodName)
                            || foodName.contains(trimmedAlias)) {
                        return food.getId();
                    }
                }
            }
        }

        return 0L;
    }

    private String sendHttpRequest(Map<String, Object> requestBody) {
        return sendHttpRequest(requestBody, qwenConfig.getEndpoint());
    }

    private String sendHttpRequest(Map<String, Object> requestBody, String apiUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + qwenConfig.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        System.out.println("请求URL: " + apiUrl);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);
            System.out.println("响应状态码: " + response.getStatusCode());
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("HTTP请求失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("调用通义千问API失败: " + e.getMessage());
        }
    }

    private void saveAiLog(Long userId, String imageUrl, String imageBase64, String aiType, String resultJson, long processTime) {
        try {
            System.out.println("开始保存AI日志, userId: " + userId + ", type: " + aiType);

            AiLog log = new AiLog();
            log.setUserId(userId);
            log.setAiType(aiType);

            if (imageUrl != null && !imageUrl.isEmpty()) {
                log.setImageUrl(imageUrl);
            } else if (imageBase64 != null && !imageBase64.isEmpty()) {
                String shortBase64 = imageBase64.length() > 100 ? imageBase64.substring(0, 100) + "..." : imageBase64;
                log.setImageUrl("base64:" + shortBase64);
            }

            log.setAiProvider("qwen");
            log.setRawResult(resultJson);
            log.setProcessTimeMs((int) processTime);
            log.setIsCorrected(0);
            log.setCreateTime(LocalDateTime.now());

            int insertResult = aiLogMapper.insert(log);
            System.out.println("插入结果: " + insertResult + ", 日志ID: " + log.getId());
        } catch (Exception e) {
            System.err.println("保存AI日志失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private RecognizeResult getMockResult() {
        RecognizeResult result = new RecognizeResult();
        List<RecognizeResult.FoodItem> foods = new ArrayList<>();

        RecognizeResult.FoodItem rice = new RecognizeResult.FoodItem();
        rice.setName("米饭");
        rice.setCalorie(new BigDecimal("116"));
        rice.setWeight(new BigDecimal("150"));
        rice.setConfidence(0.95);
        rice.setFoodId(1L);
        rice.setCarbs(new BigDecimal("25.6"));
        rice.setProtein(new BigDecimal("2.6"));
        rice.setFat(new BigDecimal("0.3"));

        RecognizeResult.FoodItem chicken = new RecognizeResult.FoodItem();
        chicken.setName("鸡胸肉");
        chicken.setCalorie(new BigDecimal("133"));
        chicken.setWeight(new BigDecimal("100"));
        chicken.setConfidence(0.88);
        chicken.setFoodId(6L);
        chicken.setCarbs(new BigDecimal("0"));
        chicken.setProtein(new BigDecimal("31"));
        chicken.setFat(new BigDecimal("2.5"));

        foods.add(rice);
        foods.add(chicken);

        result.setFoods(foods);
        result.setConfidence(0.85);

        return result;
    }

    // ==================== 体检报告分析（新增） ====================

    /**
     * 分析体检报告图片（多模态识别）
     */
    public String analyzeHealthReportImage(String imageUrl, User user) {
        String prompt = buildHealthReportImagePrompt(user);
        return qwenVLService.recognizeImage(imageUrl, prompt);
    }

    /**
     * 分析体检报告文本
     */
    public String analyzeHealthReportText(String rawText, User user) {
        String prompt = buildHealthReportTextPrompt(rawText, user);
        Map<String, Object> requestBody = buildTextRequestBody(prompt, 2000);
        String response = sendHttpRequest(requestBody);
        return extractContentFromResponse(response);
    }

    /**
     * 生成健康综合评语
     */
    public String generateHealthSummaryText(List<HealthIndicator> indicators, User user, int overallScore) {
        String prompt = buildHealthSummaryPrompt(indicators, user, overallScore);
        Map<String, Object> requestBody = buildTextRequestBody(prompt, 500);
        String response = sendHttpRequest(requestBody);
        String result = extractContentFromResponse(response);
        return result != null ? result : "综合健康状况良好，请继续保持健康的生活方式。";
    }

    /**
     * 生成单指标详细解读
     */
    public String explainSingleIndicator(HealthIndicator indicator, User user) {
        String prompt = buildIndicatorExplainPrompt(indicator, user);
        Map<String, Object> requestBody = buildTextRequestBody(prompt, 1500);
        String response = sendHttpRequest(requestBody);
        return extractContentFromResponse(response);
    }

    // ==================== 体检报告 Prompt ====================

    private String buildHealthReportImagePrompt(User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的健康数据分析师。请仔细识别这张体检报告图片，提取所有检测指标。\n\n");
        sb.append("用户档案：\n");
        sb.append("- 性别：").append(getGenderLabel(user.getGender())).append("\n");
        sb.append("- 年龄：").append(user.getAge()).append("岁\n");
        sb.append("- 身高：").append(user.getHeight()).append("cm\n");
        sb.append("- 体重：").append(user.getCurrentWeight()).append("kg\n\n");
        sb.append("请按以下JSON格式输出所有检测指标：\n");
        sb.append("{\"indicators\":[{\"name\":\"指标名称\",\"category\":\"分类(blood_routine/liver/kidney/lipid/blood_sugar/thyroid/urine/tumor_marker/other)\",\"value\":\"检测值\",\"unit\":\"单位\",\"reference_min\":数值或null,\"reference_max\":数值或null,\"status\":0,\"comment\":\"一句话通俗解读\"}]}\n\n");
        sb.append("要求：严格按报告原文提取，status根据偏离程度判断(偏离<10%为1临界，10-30%为2轻度，30-60%为3中度，>60%为4高度)，comment用通俗中文，只输出JSON");
        return sb.toString();
    }

    private String buildHealthReportTextPrompt(String rawText, User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的健康数据分析师。请分析以下体检报告文本，提取所有检测指标。\n\n");
        sb.append("用户档案：\n");
        sb.append("- 性别：").append(getGenderLabel(user.getGender())).append("\n");
        sb.append("- 年龄：").append(user.getAge()).append("岁\n");
        sb.append("- 身高：").append(user.getHeight()).append("cm\n");
        sb.append("- 体重：").append(user.getCurrentWeight()).append("kg\n\n");
        sb.append("体检报告原始文本：\n").append(rawText).append("\n\n");
        sb.append("请按以下JSON格式输出：\n");
        sb.append("{\"indicators\":[{\"name\":\"指标名称\",\"category\":\"分类\",\"value\":\"检测值\",\"unit\":\"单位\",\"reference_min\":数值或null,\"reference_max\":数值或null,\"status\":0-4,\"comment\":\"一句话解读\"}]}\n\n");
        sb.append("要求：严格按原文提取，只输出JSON");
        return sb.toString();
    }

    private String buildHealthSummaryPrompt(List<HealthIndicator> indicators, User user, int overallScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位友善的健康顾问。请根据以下体检数据，给出100字以内的综合健康评语。\n\n");
        sb.append("用户：").append(getGenderLabel(user.getGender())).append("，");
        sb.append(user.getAge()).append("岁，身高").append(user.getHeight()).append("cm，体重");
        sb.append(user.getCurrentWeight()).append("kg\n");
        sb.append("综合评分：").append(overallScore).append("分\n\n");
        sb.append("指标概览：\n");
        for (HealthIndicator hi : indicators) {
            sb.append("- ").append(hi.getIndicatorName()).append("：").append(hi.getValue());
            if (hi.getUnit() != null) sb.append(hi.getUnit());
            sb.append("（").append(getStatusLabel(hi.getStatus())).append("）\n");
        }
        sb.append("\n要求：语气积极正面，先肯定再指出问题，不要吓唬用户，只输出评语文本");
        return sb.toString();
    }

    private String buildIndicatorExplainPrompt(HealthIndicator indicator, User user) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位友善的健康顾问。请对以下异常指标进行详细解读。\n\n");
        sb.append("指标：").append(indicator.getIndicatorName()).append("\n");
        sb.append("检测值：").append(indicator.getValue()).append(" ").append(indicator.getUnit()).append("\n");
        if (indicator.getReferenceMin() != null && indicator.getReferenceMax() != null) {
            sb.append("参考范围：").append(indicator.getReferenceMin()).append("-").append(indicator.getReferenceMax());
            sb.append(" ").append(indicator.getUnit()).append("\n");
        }
        sb.append("异常程度：").append(getStatusLabel(indicator.getStatus())).append("\n");
        sb.append("用户：").append(getGenderLabel(user.getGender())).append("，").append(user.getAge()).append("岁\n\n");
        sb.append("请输出JSON：\n");
        sb.append("{\"explanation\":\"通俗解释\",\"possible_causes\":[\"原因1\",\"原因2\"],\"food_benefit\":[\"食物1\",\"食物2\"],\"food_avoid\":[\"食物1\"],\"lifestyle_tips\":[\"建议1\",\"建议2\"],\"recheck_suggest\":\"复查建议\",\"urgency\":\"low/medium/high\"}\n\n");
        sb.append("要求：语言温暖不吓人，食物要具体，只输出JSON");
        return sb.toString();
    }

    // ==================== 工具方法 ====================

    private String getGenderLabel(Integer gender) {
        if (gender == null) return "未知";
        if (gender == 1) return "男";
        if (gender == 2) return "女";
        return "未知";
    }

    private String getStatusLabel(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "正常";
            case 1: return "临界";
            case 2: return "轻度异常";
            case 3: return "中度异常";
            case 4: return "高度异常";
            default: return "未知";
        }
    }
}
