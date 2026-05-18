package com.eatnotfat.backend.controller;

import com.eatnotfat.backend.service.DashboardService;
import com.eatnotfat.backend.vo.DashboardVO;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/overview")
    public Result<DashboardVO> getDashboard(@RequestParam Long userId) {
        try {
            DashboardVO data = dashboardService.getDashboard(userId);
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取看板数据失败：" + e.getMessage());
        }
    }
}
