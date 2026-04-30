package com.eatnotfat.backend.controller.controller;

import com.eatnotfat.backend.service.FileStorageSelector;
import com.eatnotfat.backend.service.FileStorageService;
import com.eatnotfat.backend.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class UploadController {

    @Autowired
    private FileStorageSelector fileStorageSelector;

    /**
     * 上传图片（自动根据环境选择存储方式）
     */
    @PostMapping("/upload")
    public Result<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            FileStorageService storage = fileStorageSelector.getFileStorage();
            String imageUrl = storage.uploadFile(file);
            return Result.success(imageUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}