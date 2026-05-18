package com.eatnotfat.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eatnotfat.backend.dto.HealthReportUploadDTO;
import com.eatnotfat.backend.dto.IndicatorConfirmDTO;
import com.eatnotfat.backend.entity.HealthIndicator;
import com.eatnotfat.backend.entity.HealthReport;
import com.eatnotfat.backend.entity.IndicatorLibrary;
import com.eatnotfat.backend.entity.User;
import com.eatnotfat.backend.mapper.HealthIndicatorMapper;
import com.eatnotfat.backend.mapper.HealthReportMapper;
import com.eatnotfat.backend.mapper.IndicatorLibraryMapper;
import com.eatnotfat.backend.mapper.UserMapper;
import com.eatnotfat.backend.vo.HealthReportDetailVO;
import com.eatnotfat.backend.vo.HealthReportVO;
import com.eatnotfat.backend.vo.HealthScoreVO;
import com.eatnotfat.backend.vo.IndicatorTrendVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HealthReportService {

    @Autowired
    private HealthReportMapper healthReportMapper;

    @Autowired
    private HealthIndicatorMapper healthIndicatorMapper;

    @Autowired
    private IndicatorLibraryMapper indicatorLibraryMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private QwenService qwenService;

    @Value("${health.report.upload-dir:./uploads/health-reports/}")
    private String uploadDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== 分类中文名映射 ==========
    private static final Map<String, String> CATEGORY_LABEL_MAP = new LinkedHashMap<>();

    static {
        CATEGORY_LABEL_MAP.put("blood_routine", "血常规");
        CATEGORY_LABEL_MAP.put("liver", "肝功能");
        CATEGORY_LABEL_MAP.put("kidney", "肾功能");
        CATEGORY_LABEL_MAP.put("lipid", "血脂");
        CATEGORY_LABEL_MAP.put("blood_sugar", "血糖");
        CATEGORY_LABEL_MAP.put("thyroid", "甲状腺");
        CATEGORY_LABEL_MAP.put("urine", "尿常规");
        CATEGORY_LABEL_MAP.put("tumor_marker", "肿瘤标志物");
        CATEGORY_LABEL_MAP.put("other", "其他");
    }

    // ========== 状态标签映射 ==========
    private static final Map<Integer, String> STATUS_LABEL_MAP = new HashMap<>();

    static {
        STATUS_LABEL_MAP.put(0, "正常");
        STATUS_LABEL_MAP.put(1, "临界");
        STATUS_LABEL_MAP.put(2, "轻度异常");
        STATUS_LABEL_MAP.put(3, "中度异常");
        STATUS_LABEL_MAP.put(4, "高度异常");
    }

    private static final Map<Integer, String> STATUS_COLOR_MAP = new HashMap<>();

    static {
        STATUS_COLOR_MAP.put(0, "#4CAF50");
        STATUS_COLOR_MAP.put(1, "#FFD93D");
        STATUS_COLOR_MAP.put(2, "#FF9800");
        STATUS_COLOR_MAP.put(3, "#FF5722");
        STATUS_COLOR_MAP.put(4, "#f44336");
    }

    /**
     * 1. 上传体检报告
     */
    public HealthReportVO uploadReport(MultipartFile file, HealthReportUploadDTO dto) {
        String fileName = saveFile(file, dto.getUserId());
        if (fileName == null) {
            throw new RuntimeException("文件保存失败");
        }

        HealthReport report = new HealthReport();
        report.setUserId(dto.getUserId());
        report.setReportName(dto.getReportName());
        if (dto.getReportDate() != null && !dto.getReportDate().isEmpty()) {
            report.setReportDate(LocalDate.parse(dto.getReportDate()));
        }
        report.setReportType(dto.getReportType() != null ? dto.getReportType() : "综合体检");
        report.setHospital(dto.getHospital());

        String originalName = file.getOriginalFilename();
        String fileType = (originalName != null && originalName.toLowerCase().endsWith(".pdf")) ? "pdf" : "image";
        report.setFileType(fileType);
        report.setFileUrl(fileName);
        report.setStatus(0);
        report.setCreateTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());

        healthReportMapper.insert(report);

        try {
            analyzeReport(report.getId());
        } catch (Exception e) {
            e.printStackTrace();
            report.setStatus(3);
            report.setUpdateTime(LocalDateTime.now());
            healthReportMapper.updateById(report);
        }

        HealthReportVO vo = new HealthReportVO();
        report = healthReportMapper.selectById(report.getId());
        vo.setId(report.getId());
        vo.setReportName(report.getReportName());
        vo.setReportDate(report.getReportDate());
        vo.setOverallScore(report.getOverallScore());
        vo.setStatus(report.getStatus());
        vo.setCreateTime(report.getCreateTime());
        return vo;
    }

    /**
     * 2. AI分析报告（核心流程）
     */
    @Transactional
    public void analyzeReport(Long reportId) {
        HealthReport report = healthReportMapper.selectById(reportId);
        if (report == null) {
            return;
        }

        report.setStatus(1);
        report.setUpdateTime(LocalDateTime.now());
        healthReportMapper.updateById(report);

        User user = userMapper.selectById(report.getUserId());
        if (user == null) {
            report.setStatus(3);
            healthReportMapper.updateById(report);
            return;
        }

        String aiResult;
        if ("image".equals(report.getFileType())) {
            aiResult = qwenService.analyzeHealthReportImage(report.getFileUrl(), user);
        } else {
            aiResult = qwenService.analyzeHealthReportText(report.getRawText(), user);
        }

        if (aiResult == null || aiResult.isEmpty()) {
            report.setStatus(3);
            report.setUpdateTime(LocalDateTime.now());
            healthReportMapper.updateById(report);
            return;
        }

        report.setRawText(aiResult);

        List<Map<String, Object>> indicatorList = parseIndicatorList(aiResult);
        if (indicatorList == null || indicatorList.isEmpty()) {
            report.setStatus(3);
            report.setUpdateTime(LocalDateTime.now());
            healthReportMapper.updateById(report);
            return;
        }

        // 加载标准指标库
        List<IndicatorLibrary> libraryList = indicatorLibraryMapper.selectAllEnabled();
        Map<String, IndicatorLibrary> nameMap = new HashMap<>();
        for (IndicatorLibrary lib : libraryList) {
            nameMap.put(lib.getName(), lib);
            if (lib.getAliases() != null) {
                try {
                    List<String> aliases = objectMapper.readValue(
                            lib.getAliases(), new TypeReference<List<String>>() {});
                    for (String alias : aliases) {
                        nameMap.put(alias, lib);
                    }
                } catch (Exception ignored) {
                    // ignore parse error
                }
            }
        }

        // 逐个处理指标
        List<HealthIndicator> indicators = new ArrayList<>();
        for (Map<String, Object> item : indicatorList) {
            HealthIndicator hi = new HealthIndicator();
            hi.setReportId(reportId);
            hi.setUserId(report.getUserId());
            hi.setIndicatorName(getStringValue(item, "name"));
            hi.setCategory(getStringValue(item, "category"));
            hi.setValue(String.valueOf(item.get("value")));
            hi.setUnit(getStringValue(item, "unit"));
            hi.setAiComment(getStringValue(item, "comment"));

            // 匹配标准库
            String name = getStringValue(item, "name");
            IndicatorLibrary matched = nameMap.get(name);
            if (matched == null) {
                for (Map.Entry<String, IndicatorLibrary> entry : nameMap.entrySet()) {
                    if (name != null && (name.contains(entry.getKey()) || entry.getKey().contains(name))) {
                        matched = entry.getValue();
                        break;
                    }
                }
            }

            if (matched != null) {
                hi.setIndicatorCode(matched.getCode());
                boolean isMale = user.getGender() != null && user.getGender() == 1;
                if (isMale) {
                    hi.setReferenceMin(matched.getNormalMinMale());
                    hi.setReferenceMax(matched.getNormalMaxMale());
                } else {
                    hi.setReferenceMin(matched.getNormalMinFemale());
                    hi.setReferenceMax(matched.getNormalMaxFemale());
                }
                hi.setFoodRelate("{\"benefit\":" + matched.getFoodBenefit()
                        + ",\"avoid\":" + matched.getFoodAvoid() + "}");
            } else {
                if (item.get("reference_min") != null) {
                    hi.setReferenceMin(new BigDecimal(String.valueOf(item.get("reference_min"))));
                }
                if (item.get("reference_max") != null) {
                    hi.setReferenceMax(new BigDecimal(String.valueOf(item.get("reference_max"))));
                }
            }

            int aiStatus = getIntValue(item, "status");
            int localStatus = evaluateStatus(hi.getValue(), hi.getReferenceMin(), hi.getReferenceMax());
            hi.setStatus(Math.max(aiStatus, localStatus));

            indicators.add(hi);
        }

        // 插入指标并为异常指标生成详细解读
        for (HealthIndicator hi : indicators) {
            hi.setCreateTime(LocalDateTime.now());
            healthIndicatorMapper.insert(hi);

            if (hi.getStatus() >= 2) {
                try {
                    String detailJson = qwenService.explainSingleIndicator(hi, user);
                    if (detailJson != null && !detailJson.isEmpty()) {
                        String cleanJson = extractJson(detailJson);
                        Map<String, Object> detail = objectMapper.readValue(
                                cleanJson, new TypeReference<Map<String, Object>>() {});
                        hi.setAiDetail(getStringValue(detail, "explanation"));
                        hi.setAiSuggestion(buildSuggestionText(detail));

                        if (detail.containsKey("food_benefit") || detail.containsKey("food_avoid")) {
                            Map<String, Object> foodRelate = new HashMap<>();
                            foodRelate.put("benefit", detail.get("food_benefit"));
                            foodRelate.put("avoid", detail.get("food_avoid"));
                            hi.setFoodRelate(objectMapper.writeValueAsString(foodRelate));
                        }

                        healthIndicatorMapper.updateById(hi);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 生成综合评分
        Map<String, Object> scoreResult = generateScore(indicators, user);
        report.setOverallScore((Integer) scoreResult.get("overallScore"));
        report.setSummary((String) scoreResult.get("summary"));

        try {
            report.setTopConcerns(objectMapper.writeValueAsString(scoreResult.get("topConcerns")));
            report.setPositivePoints(objectMapper.writeValueAsString(scoreResult.get("positivePoints")));
            report.setCategoryScores(objectMapper.writeValueAsString(scoreResult.get("categoryScores")));
        } catch (Exception ignored) {
            // ignore
        }

        report.setStatus(2);
        report.setUpdateTime(LocalDateTime.now());
        healthReportMapper.updateById(report);
    }

    /**
     * 3. 获取报告详情
     */
    public HealthReportDetailVO getReportDetail(Long reportId, Long userId) {
        HealthReport report = healthReportMapper.selectById(reportId);
        if (report == null || !report.getUserId().equals(userId)) {
            return null;
        }

        HealthReportDetailVO vo = new HealthReportDetailVO();
        vo.setId(report.getId());
        vo.setReportName(report.getReportName());
        vo.setReportDate(report.getReportDate());
        vo.setReportType(report.getReportType());
        vo.setHospital(report.getHospital());
        vo.setFileUrl(report.getFileUrl());
        vo.setOverallScore(report.getOverallScore());
        vo.setStatus(report.getStatus());
        vo.setSummary(report.getSummary());
        vo.setDisclaimer("本分析仅供参考，不构成医疗诊断，如有异常请及时就医");

        vo.setTopConcerns(parseJsonStringList(report.getTopConcerns()));
        vo.setPositivePoints(parseJsonStringList(report.getPositivePoints()));
        vo.setCategoryScores(parseJsonIntegerMap(report.getCategoryScores()));

        LambdaQueryWrapper<HealthIndicator> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthIndicator::getReportId, reportId)
                .orderByAsc(HealthIndicator::getCategory)
                .orderByDesc(HealthIndicator::getStatus);
        List<HealthIndicator> indicators = healthIndicatorMapper.selectList(wrapper);

        List<HealthReportDetailVO.IndicatorItemVO> itemVOs = new ArrayList<>();
        for (HealthIndicator hi : indicators) {
            HealthReportDetailVO.IndicatorItemVO item = new HealthReportDetailVO.IndicatorItemVO();
            item.setId(hi.getId());
            item.setIndicatorName(hi.getIndicatorName());
            item.setIndicatorCode(hi.getIndicatorCode());
            item.setCategory(hi.getCategory());
            item.setCategoryLabel(CATEGORY_LABEL_MAP.getOrDefault(hi.getCategory(), hi.getCategory()));
            item.setValue(hi.getValue());
            item.setUnit(hi.getUnit());
            item.setStatus(hi.getStatus());
            item.setStatusLabel(STATUS_LABEL_MAP.getOrDefault(hi.getStatus(), "未知"));
            item.setStatusColor(STATUS_COLOR_MAP.getOrDefault(hi.getStatus(), "#999999"));
            item.setAiComment(hi.getAiComment());
            item.setAiDetail(hi.getAiDetail());
            item.setAiSuggestion(hi.getAiSuggestion());

            if (hi.getReferenceMin() != null && hi.getReferenceMax() != null) {
                item.setReferenceRange(hi.getReferenceMin() + "-" + hi.getReferenceMax());
            } else {
                item.setReferenceRange("定性");
            }

            if (hi.getFoodRelate() != null && !hi.getFoodRelate().isEmpty()) {
                try {
                    Map<String, List<String>> foodMap = objectMapper.readValue(
                            hi.getFoodRelate(), new TypeReference<Map<String, List<String>>>() {});
                    item.setFoodRelate(foodMap);
                } catch (Exception ignored) {
                    // ignore
                }
            }

            itemVOs.add(item);
        }

        vo.setIndicators(itemVOs);
        return vo;
    }

    /**
     * 4. 获取报告列表
     */
    public List<HealthReportVO> getReportList(Long userId) {
        LambdaQueryWrapper<HealthReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthReport::getUserId, userId)
                .orderByDesc(HealthReport::getReportDate);
        List<HealthReport> reports = healthReportMapper.selectList(wrapper);

        List<HealthReportVO> result = new ArrayList<>();
        for (HealthReport r : reports) {
            HealthReportVO vo = new HealthReportVO();
            vo.setId(r.getId());
            vo.setReportName(r.getReportName());
            vo.setReportDate(r.getReportDate());
            vo.setReportType(r.getReportType());
            vo.setHospital(r.getHospital());
            vo.setOverallScore(r.getOverallScore());
            vo.setStatus(r.getStatus());
            vo.setCreateTime(r.getCreateTime());

            LambdaQueryWrapper<HealthIndicator> iw = new LambdaQueryWrapper<>();
            iw.eq(HealthIndicator::getReportId, r.getId());
            long total = healthIndicatorMapper.selectCount(iw);
            vo.setTotalCount((int) total);

            LambdaQueryWrapper<HealthIndicator> abw = new LambdaQueryWrapper<>();
            abw.eq(HealthIndicator::getReportId, r.getId())
                    .ge(HealthIndicator::getStatus, 2);
            long abnormal = healthIndicatorMapper.selectCount(abw);
            vo.setAbnormalCount((int) abnormal);

            result.add(vo);
        }
        return result;
    }

    /**
     * 5. 确认/修正指标
     */
    @Transactional
    public void confirmIndicators(Long reportId, Long userId, List<IndicatorConfirmDTO> confirms) {
        HealthReport report = healthReportMapper.selectById(reportId);
        if (report == null || !report.getUserId().equals(userId)) {
            throw new RuntimeException("报告不存在");
        }

        for (IndicatorConfirmDTO confirm : confirms) {
            HealthIndicator indicator = healthIndicatorMapper.selectById(confirm.getIndicatorId());
            if (indicator == null || !indicator.getReportId().equals(reportId)) {
                continue;
            }

            if ("confirm".equals(confirm.getAction())) {
                // 确认无需操作
            } else if ("edit".equals(confirm.getAction())) {
                indicator.setValue(confirm.getValue());
                if (confirm.getUnit() != null) {
                    indicator.setUnit(confirm.getUnit());
                }
                int newStatus = evaluateStatus(indicator.getValue(),
                        indicator.getReferenceMin(), indicator.getReferenceMax());
                indicator.setStatus(newStatus);
                healthIndicatorMapper.updateById(indicator);
            } else if ("delete".equals(confirm.getAction())) {
                healthIndicatorMapper.deleteById(indicator.getId());
            }
        }

        recalculateScore(reportId);
    }

    /**
     * 6. 获取健康评分
     */
    public HealthScoreVO getHealthScore(Long userId) {
        LambdaQueryWrapper<HealthReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthReport::getUserId, userId)
                .eq(HealthReport::getStatus, 2)
                .orderByDesc(HealthReport::getReportDate)
                .last("LIMIT 1");
        HealthReport report = healthReportMapper.selectOne(wrapper);
        if (report == null) {
            return null;
        }

        HealthScoreVO vo = new HealthScoreVO();
        vo.setOverallScore(report.getOverallScore());
        vo.setReportDate(report.getReportDate());
        vo.setSummary(report.getSummary());
        vo.setTopConcerns(parseJsonStringList(report.getTopConcerns()));
        vo.setPositivePoints(parseJsonStringList(report.getPositivePoints()));

        Map<String, Integer> scores = parseJsonIntegerMap(report.getCategoryScores());
        if (scores != null && !scores.isEmpty()) {
            Map<String, HealthScoreVO.CategoryScore> categoryScores = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : scores.entrySet()) {
                HealthScoreVO.CategoryScore cs = new HealthScoreVO.CategoryScore();
                cs.setScore(entry.getValue());
                cs.setLabel(CATEGORY_LABEL_MAP.getOrDefault(entry.getKey(), entry.getKey()));
                categoryScores.put(entry.getKey(), cs);
            }
            vo.setCategoryScores(categoryScores);
        }

        return vo;
    }

    /**
     * 7. 指标趋势查询
     */
    public IndicatorTrendVO getIndicatorTrend(Long userId, String indicatorCode) {
        List<HealthIndicator> records = healthIndicatorMapper.selectTrendByUserAndCode(userId, indicatorCode);
        if (records == null || records.isEmpty()) {
            return null;
        }

        IndicatorTrendVO vo = new IndicatorTrendVO();
        vo.setIndicatorName(records.get(0).getIndicatorName());
        vo.setUnit(records.get(0).getUnit());
        vo.setReferenceMin(records.get(0).getReferenceMin());
        vo.setReferenceMax(records.get(0).getReferenceMax());

        List<IndicatorTrendVO.TrendPoint> trendList = new ArrayList<>();
        for (HealthIndicator hi : records) {
            IndicatorTrendVO.TrendPoint point = new IndicatorTrendVO.TrendPoint();
            HealthReport report = healthReportMapper.selectById(hi.getReportId());
            if (report != null) {
                point.setReportDate(report.getReportDate());
            }
            point.setValue(hi.getValue());
            point.setStatus(hi.getStatus());
            trendList.add(point);
        }
        vo.setTrend(trendList);
        vo.setAnalysis(generateTrendAnalysis(trendList, vo.getIndicatorName()));

        return vo;
    }

    /**
     * 8. 删除报告
     */
    @Transactional
    public void deleteReport(Long reportId, Long userId) {
        HealthReport report = healthReportMapper.selectById(reportId);
        if (report == null || !report.getUserId().equals(userId)) {
            throw new RuntimeException("报告不存在");
        }

        LambdaQueryWrapper<HealthIndicator> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthIndicator::getReportId, reportId);
        healthIndicatorMapper.delete(wrapper);

        healthReportMapper.deleteById(reportId);
    }

    // ==================== 私有辅助方法 ====================

    private String saveFile(MultipartFile file, Long userId) {
        try {
            // 使用项目根目录下的绝对路径
            String absolutePath = new File(uploadDir).getAbsolutePath();
            File dir = new File(absolutePath);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                System.out.println("创建目录: " + absolutePath + " 结果: " + created);
            }

            String originalName = file.getOriginalFilename();
            String ext = ".jpg";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf('.'));
            }
            String fileName = userId + "_" + System.currentTimeMillis() + "_"
                    + (int) (Math.random() * 9000 + 1000) + ext;

            File dest = new File(dir, fileName);
            System.out.println("保存文件到: " + dest.getAbsolutePath());
            file.transferTo(dest);

            return uploadDir + fileName;
        } catch (IOException e) {
            System.err.println("文件保存异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }


    private int evaluateStatus(String value, BigDecimal refMin, BigDecimal refMax) {
        if (refMin == null || refMax == null) {
            return 0;
        }
        try {
            double val = Double.parseDouble(value);
            double min = refMin.doubleValue();
            double max = refMax.doubleValue();

            if (val >= min && val <= max) {
                return 0;
            }

            double deviation;
            if (val < min) {
                deviation = (min - val) / min;
            } else {
                deviation = (val - max) / max;
            }

            if (deviation < 0.10) return 1;
            if (deviation < 0.30) return 2;
            if (deviation < 0.60) return 3;
            return 4;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Map<String, Object> generateScore(List<HealthIndicator> indicators, User user) {
        Map<String, Object> result = new HashMap<>();

        Map<String, List<HealthIndicator>> byCategory = new HashMap<>();
        for (HealthIndicator hi : indicators) {
            String cat = hi.getCategory();
            if (!byCategory.containsKey(cat)) {
                byCategory.put(cat, new ArrayList<>());
            }
            byCategory.get(cat).add(hi);
        }

        double baseScore = 85.0;
        for (HealthIndicator hi : indicators) {
            switch (hi.getStatus()) {
                case 1: baseScore -= 1; break;
                case 2: baseScore -= 3; break;
                case 3: baseScore -= 8; break;
                case 4: baseScore -= 15; break;
                default: baseScore += 0.5; break;
            }
        }
        int overallScore = Math.max(0, Math.min(100, (int) Math.round(baseScore)));

        Map<String, Integer> categoryScores = new LinkedHashMap<>();
        for (Map.Entry<String, List<HealthIndicator>> entry : byCategory.entrySet()) {
            double catBase = 85.0;
            for (HealthIndicator hi : entry.getValue()) {
                switch (hi.getStatus()) {
                    case 1: catBase -= 2; break;
                    case 2: catBase -= 5; break;
                    case 3: catBase -= 12; break;
                    case 4: catBase -= 20; break;
                    default: catBase += 1; break;
                }
            }
            categoryScores.put(entry.getKey(), Math.max(0, Math.min(100, (int) Math.round(catBase))));
        }

        List<String> topConcerns = new ArrayList<>();
        List<HealthIndicator> abnormalList = indicators.stream()
                .filter(i -> i.getStatus() >= 2)
                .sorted((a, b) -> b.getStatus() - a.getStatus())
                .collect(Collectors.toList());
        for (int i = 0; i < Math.min(3, abnormalList.size()); i++) {
            HealthIndicator hi = abnormalList.get(i);
            String concern = hi.getIndicatorName();
            if (hi.getAiComment() != null) {
                concern += "：" + hi.getAiComment();
            }
            topConcerns.add(concern);
        }

        List<String> positivePoints = new ArrayList<>();
        List<HealthIndicator> normalList = indicators.stream()
                .filter(i -> i.getStatus() == 0)
                .collect(Collectors.toList());
        for (int i = 0; i < Math.min(3, normalList.size()); i++) {
            positivePoints.add(normalList.get(i).getIndicatorName() + "指标正常");
        }

        String summary = qwenService.generateHealthSummaryText(indicators, user, overallScore);

        result.put("overallScore", overallScore);
        result.put("categoryScores", categoryScores);
        result.put("topConcerns", topConcerns);
        result.put("positivePoints", positivePoints);
        result.put("summary", summary);

        return result;
    }

    private void recalculateScore(Long reportId) {
        HealthReport report = healthReportMapper.selectById(reportId);
        if (report == null) return;

        LambdaQueryWrapper<HealthIndicator> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthIndicator::getReportId, reportId);
        List<HealthIndicator> indicators = healthIndicatorMapper.selectList(wrapper);

        User user = userMapper.selectById(report.getUserId());
        if (user == null) return;

        Map<String, Object> scoreResult = generateScore(indicators, user);
        report.setOverallScore((Integer) scoreResult.get("overallScore"));
        report.setSummary((String) scoreResult.get("summary"));

        try {
            report.setTopConcerns(objectMapper.writeValueAsString(scoreResult.get("topConcerns")));
            report.setPositivePoints(objectMapper.writeValueAsString(scoreResult.get("positivePoints")));
            report.setCategoryScores(objectMapper.writeValueAsString(scoreResult.get("categoryScores")));
        } catch (Exception ignored) {
            // ignore
        }

        report.setUpdateTime(LocalDateTime.now());
        healthReportMapper.updateById(report);
    }

    private List<Map<String, Object>> parseIndicatorList(String json) {
        try {
            String cleanJson = extractJson(json);
            Map<String, Object> root = objectMapper.readValue(
                    cleanJson, new TypeReference<Map<String, Object>>() {});
            Object indicators = root.get("indicators");
            if (indicators instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> list = (List<Map<String, Object>>) indicators;
                return list;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String extractJson(String text) {
        if (text == null) return "{}";

        // 先移除 markdown 代码块标记（```json 或 ```）
        String cleaned = text.replaceAll("```[\\w]*\\n?", "").replaceAll("```", "").trim();

        // 然后提取 JSON
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return "{}";
    }

    private List<String> parseJsonStringList(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private Map<String, Integer> parseJsonIntegerMap(String json) {
        if (json == null || json.isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String buildSuggestionText(Map<String, Object> detail) {
        StringBuilder sb = new StringBuilder();

        appendListSection(sb, "可能原因", detail, "possible_causes");
        appendListSection(sb, "推荐食物", detail, "food_benefit");
        appendListSection(sb, "需避免", detail, "food_avoid");
        appendListSection(sb, "生活建议", detail, "lifestyle_tips");

        if (detail.containsKey("recheck_suggest")) {
            sb.append("【复查建议】").append(detail.get("recheck_suggest"));
        }

        return sb.toString();
    }

    private void appendListSection(StringBuilder sb, String title,
                                   Map<String, Object> detail, String key) {
        if (detail.containsKey(key)) {
            Object val = detail.get(key);
            if (val instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) val;
                if (list != null && !list.isEmpty()) {
                    sb.append("【").append(title).append("】");
                    sb.append(String.join("、", list));
                    sb.append("\n");
                }
            }
        }
    }

    private String generateTrendAnalysis(List<IndicatorTrendVO.TrendPoint> trend,
                                         String indicatorName) {
        if (trend == null || trend.size() < 2) {
            return "数据不足，至少需要两次报告才能分析趋势。";
        }

        IndicatorTrendVO.TrendPoint first = trend.get(0);
        IndicatorTrendVO.TrendPoint last = trend.get(trend.size() - 1);

        try {
            double firstVal = Double.parseDouble(first.getValue());
            double lastVal = Double.parseDouble(last.getValue());

            if (last.getStatus() == 0 && first.getStatus() > 0) {
                return indicatorName + "已从异常恢复到正常范围，趋势良好，请继续保持。";
            } else if (last.getStatus() > first.getStatus()) {
                return indicatorName + "有恶化趋势（从" + first.getValue()
                        + "变为" + last.getValue() + "），建议关注并就医咨询。";
            } else if (last.getStatus() < first.getStatus()) {
                return indicatorName + "有好转趋势（从" + first.getValue()
                        + "变为" + last.getValue() + "），请继续保持当前的生活方式。";
            } else if (lastVal > firstVal) {
                return indicatorName + "数值有所上升（从" + first.getValue()
                        + "变为" + last.getValue() + "），请持续关注。";
            } else {
                return indicatorName + "数值有所下降（从" + first.getValue()
                        + "变为" + last.getValue() + "），请持续关注。";
            }
        } catch (NumberFormatException e) {
            return "该指标的趋势数据暂不支持自动分析。";
        }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? String.valueOf(val) : null;
    }

    private int getIntValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return 0;
    }
}
