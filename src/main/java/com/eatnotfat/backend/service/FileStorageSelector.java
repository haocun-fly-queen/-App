package com.eatnotfat.backend.service;

import com.eatnotfat.backend.config.StorageConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class FileStorageSelector {

    @Autowired
    private StorageConfig storageConfig;

    @Autowired
    @Qualifier("localFileStorage")
    private FileStorageService localFileStorage;

    @Autowired
    @Qualifier("ossFileStorage")
    private FileStorageService ossFileStorage;

    public FileStorageService getFileStorage() {
        if (storageConfig.isProd()) {
            return ossFileStorage;
        } else {
            return localFileStorage;
        }
    }
}