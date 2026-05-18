package com.eatnotfat.backend.controller;

import com.eatnotfat.backend.dto.HealthReportUploadDTO;
import com.eatnotfat.backend.dto.IndicatorConfirmDTO;
import com.eatnotfat.backend.service.HealthReportService;
import com.eatnotfat.backend.vo.HealthReportDetailVO;
import com.eatnotfat.backend.vo.HealthReportVO;
import com.eatnotfat.backend.vo.HealthScoreVO;
import com.eatnotfat.backend.vo.IndicatorTrendVO;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/health")
public class HealthReportController {

    @Autowired
    private HealthReportService healthReportService;

    @PostMapping("/report/upload")
    public Result<HealthReportVO> uploadReport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestParam("reportName") String reportName,
            @RequestParam(value = "reportDate", required = false) String reportDate,
            @RequestParam(value = "reportType", required = false) String reportType,
            @RequestParam(value = "hospital", required = false) String hospital) {

        HealthReportUploadDTO dto = new HealthReportUploadDTO();
        dto.setUserId(userId);
        dto.setReportName(reportName);
        dto.setReportDate(reportDate);
        dto.setReportType(reportType);
        dto.setHospital(hospital);

        HealthReportVO vo = healthReportService.uploadReport(file, dto);
        return Result.success(vo);
    }

    @GetMapping("/report/{id}")
    public Result<HealthReportDetailVO> getReportDetail(
            @PathVariable Long id,
            @RequestParam Long userId) {
        HealthReportDetailVO vo = healthReportService.getReportDetail(id, userId);
        if (vo == null) {
            return Result.error("报告不存在");
        }
        return Result.success(vo);
    }

    @GetMapping("/reports")
    public Result<List<HealthReportVO>> getReportList(@RequestParam Long userId) {
        List<HealthReportVO> list = healthReportService.getReportList(userId);
        return Result.success(list);
    }

    @PostMapping("/report/{id}/confirm")
    public Result<String> confirmIndicators(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestBody List<IndicatorConfirmDTO> confirms) {
        healthReportService.confirmIndicators(id, userId, confirms);
        return Result.success("确认成功");
    }

    @GetMapping("/score")
    public Result<HealthScoreVO> getHealthScore(@RequestParam Long userId) {
        HealthScoreVO vo = healthReportService.getHealthScore(userId);
        if (vo == null) {
            return Result.success(null);
        }
        return Result.success(vo);
    }


    @GetMapping("/indicator/trend")
    public Result<IndicatorTrendVO> getIndicatorTrend(
            @RequestParam Long userId,
            @RequestParam String code) {
        IndicatorTrendVO vo = healthReportService.getIndicatorTrend(userId, code);
        if (vo == null) {
            return Result.error("暂无该指标的历史数据");
        }
        return Result.success(vo);
    }

    @DeleteMapping("/report/{id}")
    public Result<String> deleteReport(
            @PathVariable Long id,
            @RequestParam Long userId) {
        healthReportService.deleteReport(id, userId);
        return Result.success("删除成功");
    }
}
