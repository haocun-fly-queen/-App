package com.eatnotfat.backend.service.impl;

import com.eatnotfat.backend.config.StorageConfig;
import com.eatnotfat.backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service("localFileStorage")
public class LocalFileStorageServiceImpl implements FileStorageService {

    @Autowired
    private StorageConfig storageConfig;

    @Override
    public String uploadFile(MultipartFile file) {
        try {
            // 获取原文件名后缀
            String originalFilename = file.getOriginalFilename();
            String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 生成新文件名
            String newFileName = UUID.randomUUID().toString() + suffix;

            // 按日期分目录
            String dateDir = java.time.LocalDate.now().toString().replace("-", "/");

            // 获取项目根目录的绝对路径
            String projectPath = System.getProperty("user.dir");
            String uploadDir = projectPath + "/uploads/" + dateDir;

            // 创建目录（确保父目录也存在）
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                System.out.println("创建目录: " + uploadDir + ", 成功: " + created);
            }

            // 保存文件
            File destFile = new File(uploadDir + "/" + newFileName);
            file.transferTo(destFile);

            System.out.println("文件保存成功: " + destFile.getAbsolutePath());

            // 返回访问URL
            return "http://localhost:8080/uploads/" + dateDir + "/" + newFileName;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("本地文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            // 从URL中提取文件路径
            String path = fileUrl.replace("http://localhost:8080/uploads/", "");
            String projectPath = System.getProperty("user.dir");
            File file = new File(projectPath + "/uploads/" + path);
            if (file.exists()) {
                file.delete();
                System.out.println("文件删除成功: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}