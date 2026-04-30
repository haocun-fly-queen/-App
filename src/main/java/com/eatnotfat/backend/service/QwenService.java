package com.eatnotfat.backend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.eatnotfat.backend.config.QwenConfig;
import com.eatnotfat.backend.dto.DietPlanRequest;
import com.eatnotfat.backend.entity.AiLog;
import com.eatnotfat.backend.entity.FoodStandard;
import com.eatnotfat.backend.mapper.AiLogMapper;
import com.eatnotfat.backend.mapper.FoodStandardMapper;
import com.eatnotfat.backend.vo.DietPlanResult;
import com.eatnotfat.backend.vo.RecognizeResult;
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
import java.util.stream.Collectors;

@Service
public class QwenService {

    @Autowired
    private QwenConfig qwenConfig;

    @Autowired
    private FoodStandardMapper foodStandardMapper;

    @Autowired
    private AiLogMapper aiLogMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    // ==================== 原有：食物识别 ====================

    /**
     * 调用通义千问识别食物（支持 URL 或 Base64）
     */
    public RecognizeResult recognizeFood(String imageUrl, String imageBase64, Long userId) {
        long startTime = System.currentTimeMillis();

        try {
            System.out.println("========== 调用通义千问识别食物 ==========");
            System.out.println("userId: " + userId);

            Map<String, Object> requestBody = buildRecognizeRequestBody(imageUrl, imageBase64);
            System.out.println("请求参数: " + JSON.toJSONString(requestBody));

            String response = sendHttpRequest(requestBody);
            System.out.println("API返回: " + response);

            RecognizeResult result = parseRecognizeResponse(response);
            matchLocalFoods(result);

            long processTime = System.currentTimeMillis() - startTime;
            System.out.println("处理耗时: " + processTime + "ms");

            System.out.println("========== 准备保存AI日志 ==========");
            saveAiLog(userId, imageUrl, imageBase64, "recognize", JSON.toJSONString(result), processTime);
            System.out.println("========== AI日志保存完成 ==========");

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("调用通义千问失败: " + e.getMessage());
        }
    }

    // ==================== 新增：AI 饮食规划 ====================

    /**
     * AI 智能饮食规划 - 根据用户Profile和已摄入情况生成下一餐建议
     */
    public DietPlanResult generateDietPlan(DietPlanRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            System.out.println("========== 调用通义千问饮食规划 ==========");
            System.out.println("用户ID: " + request.getUserId());
            System.out.println("目标餐次: " + request.getTargetMeal().getName());

            // 构建规划Prompt
            String prompt = buildDietPlanPrompt(request);
            System.out.println("规划Prompt长度: " + prompt.length());

            // 构建请求体（纯文本，不需要图片）
            Map<String, Object> requestBody = buildTextRequestBody(prompt);
            System.out.println("请求参数: " + JSON.toJSONString(requestBody));

            // 发送请求
            String response = sendHttpRequest(requestBody);
            System.out.println("API返回: " + response);

            // 解析结果
            DietPlanResult result = parseDietPlanResponse(response, request);

            long processTime = System.currentTimeMillis() - startTime;
            System.out.println("规划耗时: " + processTime + "ms");

            // 保存日志
            saveAiLog(request.getUserId(), null, null, "diet-plan", JSON.toJSONString(result), processTime);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("AI规划失败，使用本地降级方案");
            return generateFallbackPlan(request);
        }
    }

    // ==================== 饮食规划：Prompt构建 ====================

    /**
     * 构建饮食规划Prompt
     */
    private String buildDietPlanPrompt(DietPlanRequest req) {
        DietPlanRequest.UserProfile p = req.getProfile();
        DietPlanRequest.CalorieStatus c = req.getCalorieStatus();
        DietPlanRequest.DietRestrictions r = req.getRestrictions();

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的中国注册营养师和AI饮食规划师。请根据以下用户信息，生成下一餐的详细饮食建议。\n\n");

        // 用户基础信息
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

        // 饮食限制
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

        // 今日热量状态（核心决策依据）
        prompt.append("【今日热量状态 - 这是最重要的决策依据】\n");
        prompt.append("- 每日热量目标：").append(c.getTarget()).append("kcal\n");
        prompt.append("- 已摄入热量：").append(c.getConsumed()).append("kcal\n");
        prompt.append("- 剩余可用热量：").append(c.getRemaining()).append("kcal\n");
        prompt.append("- 完成进度：").append(String.format("%.1f", c.getProgressPercent())).append("%\n\n");

        // 已记录餐次
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

        // 目标餐次
        prompt.append("【需要规划的餐次】\n");
        prompt.append("→ ").append(req.getTargetMeal().getName()).append("\n\n");

        // 核心约束条件
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
            prompt.append("   → 注意：前面餐次可能吃多了，本餐要减量\n");
        } else {
            prompt.append("🟢 今日热量剩余 ").append(remaining).append("kcal，空间充足\n");
            prompt.append("   → 按正常份量规划\n");
        }

        // 根据健康目标调整
        if (p.getGoalType() == 1) { // 减脂
            prompt.append("- 减脂策略：控制碳水在40-50%，提高蛋白质到25-30%，脂肪20-25%\n");
            prompt.append("- 优先选择：低GI主食、瘦肉、高纤维蔬菜\n");
            prompt.append("- 避免：油炸食品、精制糖、含糖饮料\n");
        } else if (p.getGoalType() == 2) { // 增肌
            prompt.append("- 增肌策略：碳水50-55%，蛋白质25-30%（每公斤体重1.6-2.2g），脂肪20-25%\n");
            prompt.append("- 优先选择：优质蛋白（鸡胸肉、鱼、蛋、乳清）、复合碳水\n");
            prompt.append("- 训练后补充：快碳+蛋白质\n");
        } else { // 保持
            prompt.append("- 保持策略：碳水50-55%，蛋白质15-20%，脂肪25-30%\n");
            prompt.append("- 均衡搭配：主食+蛋白质+蔬菜，比例约 1:1:2\n");
        }

        // 餐次-specific建议
        String mealName = req.getTargetMeal().getName();
        if ("早餐".equals(mealName)) {
            prompt.append("- 早餐原则：必吃！唤醒代谢，碳水+蛋白质为主\n");
            prompt.append("- 推荐结构：全麦主食 + 蛋/奶 + 少量坚果/水果\n");
        } else if ("午餐".equals(mealName)) {
            prompt.append("- 午餐原则：承上启下，热量最高的一餐\n");
            prompt.append("- 推荐结构：主食 + 优质蛋白 + 大量蔬菜\n");
        } else if ("晚餐".equals(mealName)) {
            prompt.append("- 晚餐原则：清淡少油，睡前3小时完成\n");
            prompt.append("- 推荐结构：少量主食/杂粮 + 易消化蛋白 + 蔬菜\n");
            prompt.append("- 避免：高脂肪、难消化、过量碳水\n");
        } else if ("加餐".equals(mealName)) {
            prompt.append("- 加餐原则：填补饥饿，防止暴食\n");
            prompt.append("- 推荐：水果、酸奶、少量坚果（控制在100-150kcal）\n");
        }
        prompt.append("\n");

        // 输出格式要求
        prompt.append("【输出要求 - 必须严格按以下JSON格式返回】\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"mealType\": \"餐次名称（如：午餐）\",\n");
        prompt.append("  \"totalCalories\": 整数,\n");
        prompt.append("  \"foods\": [\n");
        prompt.append("    {\"name\": \"具体食物名称\", \"amount\": \"具体份量（如：1碗/150g）\", \"calories\": 整数, \"image\": \"\"},\n");
        prompt.append("    {\"name\": \"具体食物名称\", \"amount\": \"具体份量\", \"calories\": 整数, \"image\": \"\"}\n");
        prompt.append("  ],\n");
        prompt.append("  \"nutrition\": {\"carbs\": 整数(0-100), \"protein\": 整数(0-100), \"fat\": 整数(0-100)},\n");
        prompt.append("  \"reason\": \"详细的推荐理由，必须包含：1)为什么选这些食物 2)如何适配用户目标 3)与已摄入餐次的平衡关系\"\n");
        prompt.append("}\n");
        prompt.append("```\n\n");

        prompt.append("【重要提醒】\n");
        prompt.append("1. 食物必须是中式家常菜，名称要具体（如'糙米饭'而不是'主食'）\n");
        prompt.append("2. 份量要具体可操作（如'1碗约200g'而不是'适量'）\n");
        prompt.append("3. 总热量必须精确计算，与foods中各项之和一致\n");
        prompt.append("4. 必须遵守用户的饮食限制和过敏源，这是硬性约束\n");
        prompt.append("5. 推荐理由要个性化，提及用户的具体情况，不要泛泛而谈\n");
        prompt.append("6. 只返回JSON，不要有任何其他文字说明");

        return prompt.toString();
    }

    // ==================== 饮食规划：请求构建 ====================

    /**
     * 构建纯文本请求体（用于饮食规划，不需要图片）
     */
    private Map<String, Object> buildTextRequestBody(String prompt) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", qwenConfig.getModel());

        Map<String, Object> input = new HashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();

        // 系统角色设定
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一位专业的中国注册营养师，擅长根据用户的身体状况、健康目标和饮食记录，制定个性化的饮食方案。你的建议必须科学、实用、符合中式饮食习惯。");
        messages.add(systemMessage);

        // 用户请求
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        input.put("messages", messages);
        request.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("result_format", "message");
        parameters.put("max_tokens", 1500);
        request.put("parameters", parameters);

        return request;
    }

    // ==================== 饮食规划：响应解析 ====================

    /**
     * 解析饮食规划响应
     */
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

            // 解析食物列表
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

            // 解析营养占比
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

            // 验证热量一致性
            int calcTotal = foods.stream().mapToInt(f -> f.getCalories() != null ? f.getCalories() : 0).sum();
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

    /**
     * 从响应中提取content
     */
    private String extractContentFromResponse(String response) {
        try {
            JSONObject json = JSONObject.parseObject(response);
            JSONObject output = json.getJSONObject("output");
            if (output == null) return response;

            JSONArray choices = output.getJSONArray("choices");
            if (choices == null || choices.size() == 0) return response;

            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            return message.getString("content");
        } catch (Exception e) {
            return response;
        }
    }

    /**
     * 生成降级方案（AI失败时使用）
     */
    private DietPlanResult generateFallbackPlan(DietPlanRequest request) {
        DietPlanRequest.UserProfile p = request.getProfile();
        DietPlanRequest.CalorieStatus c = request.getCalorieStatus();
        String mealName = request.getTargetMeal().getName();
        int remaining = c.getRemaining();

        DietPlanResult result = new DietPlanResult();
        result.setMealType(mealName);

        List<DietPlanResult.FoodItem> foods = new ArrayList<>();

        // 根据剩余热量和目标调整
        int targetMealCal;
        if (remaining < 0) {
            targetMealCal = 150;
        } else if (remaining < 300) {
            targetMealCal = 250;
        } else {
            targetMealCal = switch (mealName) {
                case "早餐" -> Math.min(500, (int)(remaining * 0.35));
                case "午餐" -> Math.min(700, (int)(remaining * 0.4));
                case "晚餐" -> Math.min(500, (int)(remaining * 0.25));
                default -> 150;
            };
        }

        // 根据饮食偏好选择食物
        boolean isVegetarian = "vegetarian".equals(p.getDietPreference());
        boolean isHalal = "halal".equals(p.getDietPreference());

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
        int total = foods.stream().mapToInt(f -> f.getCalories() != null ? f.getCalories() : 0).sum();
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

    // ==================== 原有：食物识别相关方法 ====================

    /**
     * 构建食物识别请求体
     */
    private Map<String, Object> buildRecognizeRequestBody(String imageUrl, String imageBase64) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", qwenConfig.getModel());

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

    /**
     * 构建食物识别提示词
     */
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

    /**
     * 解析食物识别响应
     */
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

        double avgConfidence = foods.stream()
                .mapToDouble(RecognizeResult.FoodItem::getConfidence)
                .average()
                .orElse(0.85);

        result.setFoods(foods);
        result.setConfidence(avgConfidence);

        return result;
    }

    /**
     * 从内容中提取JSON
     */
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
                // 不是JSON数组，继续处理
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

    /**
     * 匹配本地食物库
     */
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

    /**
     * 查找匹配的食物ID
     */
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

    /**
     * 发送 HTTP 请求到通义千问
     */
    private String sendHttpRequest(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + qwenConfig.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String apiUrl = qwenConfig.getApiUrl();
        System.out.println("请求URL: " + apiUrl);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            System.out.println("响应状态码: " + response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("HTTP请求失败: " + response.getStatusCode() + ", 响应: " + response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("调用通义千问API失败: " + e.getMessage());
        }
    }

    /**
     * 保存AI日志（通用）
     */
    private void saveAiLog(Long userId, String imageUrl, String imageBase64, String aiType, String resultJson, long processTime) {
        try {
            System.out.println("开始保存AI日志, userId: " + userId + ", type: " + aiType);

            AiLog log = new AiLog();
            log.setUserId(userId);
            log.setAiType(aiType); // "recognize" 或 "diet-plan"

            if (imageUrl != null && !imageUrl.isEmpty()) {
                log.setImageUrl(imageUrl);
            } else if (imageBase64 != null && !imageBase64.isEmpty()) {
                String shortBase64 = imageBase64.length() > 100 ? imageBase64.substring(0, 100) + "..." : imageBase64;
                log.setImageUrl("base64:" + shortBase64);
            }

            log.setAiProvider("qwen");
            log.setRawResult(resultJson);
            System.out.println("rawResult长度: " + resultJson.length());

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

    /**
     * 模拟返回（解析失败时使用）
     */
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
}