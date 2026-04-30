package com.eatnotfat.backend.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.eatnotfat.backend.config.StorageConfig;
import com.eatnotfat.backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service("ossFileStorage")
public class OssFileStorageServiceImpl implements FileStorageService {

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
            String objectName = "food-images/" + dateDir + "/" + newFileName;

            // 创建OSS客户端
            OSS ossClient = new OSSClientBuilder().build(
                    storageConfig.getOssEndpoint(),
                    storageConfig.getOssAccessKeyId(),
                    storageConfig.getOssAccessKeySecret()
            );

            // 上传文件
            try (InputStream inputStream = file.getInputStream()) {
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        storageConfig.getOssBucketName(),
                        objectName,
                        inputStream
                );
                ossClient.putObject(putObjectRequest);
            }

            // 关闭客户端
            ossClient.shutdown();

            // 返回访问URL
            return "https://" + storageConfig.getOssBucketName() + "." + storageConfig.getOssEndpoint() + "/" + objectName;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("OSS文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            // 从URL中提取objectName
            String baseUrl = "https://" + storageConfig.getOssBucketName() + "." + storageConfig.getOssEndpoint() + "/";
            String objectName = fileUrl.replace(baseUrl, "");

            // 创建OSS客户端并删除
            OSS ossClient = new OSSClientBuilder().build(
                    storageConfig.getOssEndpoint(),
                    storageConfig.getOssAccessKeyId(),
                    storageConfig.getOssAccessKeySecret()
            );

            ossClient.deleteObject(storageConfig.getOssBucketName(), objectName);
            ossClient.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}