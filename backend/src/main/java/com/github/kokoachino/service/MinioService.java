package com.github.kokoachino.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;


/**
 * MinIO 服务接口
 *
 * @author Kokoa_Chino
 * @date 2026-02-09
 */
public interface MinioService {

    /**
     * 上传文件
     *
     * @param file     文件
     * @param objectKey 对象Key
     * @return 访问URL
     */
    String uploadFile(MultipartFile file, String objectKey);

    /**
     * 获取文件访问URL
     *
     * @param objectKey 对象Key
     * @return 访问URL
     */
    String getFileUrl(String objectKey);

    /**
     * 获取带签名的临时访问URL
     *
     * @param objectKey 对象Key
     * @param expiry    过期时间（秒）
     * @return 临时访问URL
     */
    String getPresignedUrl(String objectKey, int expiry);

    /**
     * 删除文件
     *
     * @param objectKey 对象Key
     */
    void deleteFile(String objectKey);

    /**
     * 下载文件
     *
     * @param objectKey 对象Key
     * @return 文件流
     */
    InputStream downloadFile(String objectKey);
}
