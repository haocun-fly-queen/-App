package com.eatnotfat.backend.controller;

import com.eatnotfat.backend.service.DietPlanService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/diet-plan")
public class DietPlanController {

    @Autowired
    private DietPlanService dietPlanService;

    /**
     * 获取饮食规划
     */
    @GetMapping("/plan")
    public Result<Map<String, Object>> getDietPlan(@RequestParam Long userId) {
        try {
            Map<String, Object> plan = dietPlanService.getDietPlan(userId);
            return Result.success(plan);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(e.getMessage());
        }
    }
}