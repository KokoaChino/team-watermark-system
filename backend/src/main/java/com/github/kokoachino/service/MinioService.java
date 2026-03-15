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
     * 上传文件并自动生成 object key
     *
     * @param file 文件
     * @param prefix 对象 key 前缀（如 "images/"）
     * @return 完整的文件访问 URL
     */
    String uploadFileWithAutoKey(MultipartFile file, String prefix);

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

    /**
     * 获取文件流（别名方法，与 downloadFile 相同）
     *
     * @param objectKey 对象Key
     * @return 文件流
     */
    default InputStream getFileStream(String objectKey) {
        return downloadFile(objectKey);
    }
}
