package com.joker.ai.exam.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    String uploadFile(MultipartFile file, String folder) throws Exception;
}
