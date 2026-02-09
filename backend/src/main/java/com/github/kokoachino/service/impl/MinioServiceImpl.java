package com.github.kokoachino.service.impl;

import com.github.kokoachino.config.MinioConfig;
import com.github.kokoachino.service.MinioService;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * MinIO 服务实现类
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioServiceImpl implements MinioService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @Override
    public String uploadFile(MultipartFile file, String objectKey) {
        try {
            // 确保存储桶存在
            ensureBucketExists();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return getFileUrl(objectKey);
        } catch (Exception e) {
            log.error("上传文件到MinIO失败", e);
            throw new RuntimeException("上传文件失败", e);
        }
    }

    @Override
    public String getFileUrl(String objectKey) {
        return String.format("%s/%s/%s", minioConfig.getEndpoint(), minioConfig.getBucketName(), objectKey);
    }

    @Override
    public String getPresignedUrl(String objectKey, int expiry) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .expiry(expiry, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("获取MinIO预签名URL失败", e);
            throw new RuntimeException("获取文件访问链接失败", e);
        }
    }

    @Override
    public void deleteFile(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("删除MinIO文件失败", e);
            throw new RuntimeException("删除文件失败", e);
        }
    }

    @Override
    public InputStream downloadFile(String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("下载MinIO文件失败", e);
            throw new RuntimeException("下载文件失败", e);
        }
    }

    /**
     * 生成字体文件的ObjectKey
     *
     * @param teamId   团队ID
     * @param fileName 原始文件名
     * @return ObjectKey
     */
    public String generateFontObjectKey(Integer teamId, String fileName) {
        String extension = getFileExtension(fileName);
        return String.format("fonts/team_%d/%s.%s", teamId, UUID.randomUUID().toString().replace("-", ""), extension);
    }

    /**
     * 确保存储桶存在
     */
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build()
            );
            // 设置存储桶为公开可读
            String policy = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":\"*\",\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::" + minioConfig.getBucketName() + "/*\"]}]}";
            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .config(policy)
                            .build()
            );
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
