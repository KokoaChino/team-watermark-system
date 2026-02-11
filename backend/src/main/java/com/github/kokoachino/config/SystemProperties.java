package com.github.kokoachino.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


/**
 * 系统配置属性类
 *
 * @author kokoachino
 * @date 2026-02-02
 */
@Data
@Component
@ConfigurationProperties(prefix = "system")
public class SystemProperties {

    /**
     * 初始点数
     */
    private Integer initialPoints;

    /**
     * 点数单价（元/点）
     */
    private Double pointPrice;

    /**
     * 图形验证码配置
     */
    private CaptchaConfig captcha = new CaptchaConfig();

    /**
     * 邮箱验证码配置
     */
    private EmailCodeConfig emailCode = new EmailCodeConfig();

    /**
     * 批量任务配置
     */
    private BatchTaskConfig batchTask = new BatchTaskConfig();

    /**
     * 二维码配置
     */
    private QrCodeConfig qrCode = new QrCodeConfig();

    @Data
    public static class CaptchaConfig {
        /**
         * 验证码宽度
         */
        private Integer width;

        /**
         * 验证码高度
         */
        private Integer height;

        /**
         * 验证码字符数
         */
        private Integer codeLength;

        /**
         * 干扰线宽度
         */
        private Integer lineCount;

        /**
         * 有效期（分钟）
         */
        private Integer expiration;
    }

    @Data
    public static class EmailCodeConfig {
        /**
         * 有效期（分钟）
         */
        private Integer expiration;
    }

    @Data
    public static class BatchTaskConfig {
        /**
         * 结果ZIP文件有效期（小时）
         */
        private Integer resultZipExpiry;

        /**
         * 单个任务最大图片数量
         */
        private Integer maxImagesPerTask;
    }

    @Data
    public static class QrCodeConfig {
        /**
         * 二维码宽度
         */
        private Integer width;

        /**
         * 二维码高度
         */
        private Integer height;

        /**
         * 边距
         */
        private Integer margin;
    }
}
