package com.github.kokoachino.common.util;

import cn.hutool.extra.qrcode.QrConfig;
import com.github.kokoachino.config.SystemProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.Base64;


/**
 * 二维码工具类
 *
 * @author Kokoa_Chino
 * @date 2026-02-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QrCodeUtil {

    private final SystemProperties systemProperties;
    private static SystemProperties staticSystemProperties;

    @PostConstruct
    public void init() {
        staticSystemProperties = systemProperties;
    }

    /**
     * 生成二维码 Base64 图片
     *
     * @param content 二维码内容
     * @return Base64 编码的二维码图片（带 data:image/png;base64, 前缀）
     */
    public static String generateBase64QrCode(String content) {
        try {
            SystemProperties.QrCodeConfig config = staticSystemProperties.getQrCode();
            QrConfig qrConfig = new QrConfig(config.getWidth(), config.getHeight());
            qrConfig.setMargin(config.getMargin());
            qrConfig.setForeColor(Color.BLACK);
            qrConfig.setBackColor(Color.WHITE);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            cn.hutool.extra.qrcode.QrCodeUtil.generate(content, qrConfig, "png", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            log.error("生成二维码失败", e);
            throw new RuntimeException("生成二维码失败", e);
        }
    }
}
