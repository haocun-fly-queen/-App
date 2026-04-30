package com.eatnotfat.backend.controller.controller;

import com.eatnotfat.backend.dto.RecognizeRequest;
import com.eatnotfat.backend.service.QwenService;
import com.eatnotfat.backend.vo.Result;
import com.eatnotfat.backend.vo.RecognizeResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private QwenService qwenService;

    /**
     * AI食物识别接口 - 调用通义千问（支持 URL 和 Base64）
     */
    @PostMapping("/recognize")
    public Result<RecognizeResult> recognizeFood(@RequestBody RecognizeRequest request) {
        try {
            System.out.println("========== AI识别请求 ==========");

            RecognizeResult result;

            // 优先使用 Base64 图片
            if (request.getImageBase64() != null && !request.getImageBase64().isEmpty()) {
                System.out.println("使用 Base64 图片");
                result = qwenService.recognizeFood(null, request.getImageBase64(), request.getUserId());
            }
            // 其次使用 URL 图片
            else if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
                System.out.println("使用 URL 图片: " + request.getImageUrl());
                result = qwenService.recognizeFood(request.getImageUrl(), null, request.getUserId());
            }
            else {
                return Result.error("没有提供图片");
            }

            System.out.println("识别结果: " + (result != null ? result.getFoods().size() + "种食物" : "无结果"));

            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("识别失败：" + e.getMessage());
        }
    }
}