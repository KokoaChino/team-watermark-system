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

    private CaptchaConfig captcha = new CaptchaConfig();

    private EmailCodeConfig emailCode = new EmailCodeConfig();

    private BatchTaskConfig batchTask = new BatchTaskConfig();

    private QrCodeConfig qrCode = new QrCodeConfig();

    private TemplateConfig template = new TemplateConfig();

    private PointConfig point = new PointConfig();

    @Data
    public static class CaptchaConfig {
        private Integer width;
        private Integer height;
        private Integer codeLength;
        private Integer lineCount;
        private Integer expiration;
    }

    @Data
    public static class EmailCodeConfig {
        private Integer expiration;
    }

    @Data
    public static class BatchTaskConfig {
        private Integer resultZipExpiry;
        private Integer maxImagesPerTask;
    }

    @Data
    public static class QrCodeConfig {
        private Integer width;
        private Integer height;
        private Integer margin;
    }

    @Data
    public static class TemplateConfig {
        private Integer defaultWidth;
        private Integer defaultHeight;
        private String defaultBackgroundColor;
        private String defaultName;
    }

    @Data
    public static class PointConfig {
        private Integer initialPoints;
        private Double price;
        private Integer maxPointsPerOrder;
    }
}
