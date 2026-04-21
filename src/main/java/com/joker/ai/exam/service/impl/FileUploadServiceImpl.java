package com.joker.ai.exam.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.IdUtil;
import com.joker.ai.exam.config.MinioProperties;
import com.joker.ai.exam.service.FileUploadService;
import io.minio.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;


@Log4j2
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    @Override
    public String uploadFile(MultipartFile file, String folder) throws Exception {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().
                bucket(minioProperties.getBucketName())
                .build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
            String config = """
                        {
                              "Statement" : [ {
                                "Action" : "s3:GetObject",
                                "Effect" : "Allow",
                                "Principal" : "*",
                                "Resource" : "arn:aws:s3:::%s/*"
                              } ],
                              "Version" : "2012-10-17"
                        }
                    """.formatted(minioProperties.getBucketName());
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(minioProperties.getBucketName())
                    .config(config)
                    .build());
        }
        String yyyyMMdd = DateFormatUtils.format(new Date(), "yyyyMMdd");
        String string = IdUtil.fastSimpleUUID();
        String objectName = folder + "/"+ yyyyMMdd + "/" + string + "_" + file.getOriginalFilename();
        log.info("文件名称：{}",objectName);
         minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());

        String filePath = String.join("/", minioProperties.getEndpoint(), minioProperties.getBucketName(), objectName);
        return filePath;
    }


}
