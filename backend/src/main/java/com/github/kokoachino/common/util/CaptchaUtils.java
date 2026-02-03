package com.github.kokoachino.common.util;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.github.kokoachino.config.SystemProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.UUID;


/**
 * 验证码工具类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Component
@RequiredArgsConstructor
public class CaptchaUtils {

    private final SystemProperties systemProperties;

    /**
     * 验证码结果封装类（包含需要存储的 code 和需要返回给前端的 base64、key）
     */
    public record CaptchaResult(String key, String code, String base64) {}

    /**
     * 创建图形验证码
     * 从配置文件读取验证码参数，并自动生成 UUID 作为 key
     *
     * @return 验证码结果，包含 key、code（用于存储）和 base64（用于返回前端）
     */
    public CaptchaResult createCaptcha() {
        SystemProperties.CaptchaConfig config = systemProperties.getCaptcha();
        // 创建图形验证码：宽、高、验证码字符数、干扰线宽度
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(
                config.getWidth(),
                config.getHeight(),
                config.getCodeLength(),
                config.getLineCount()
        );
        // 生成 UUID 作为 key
        String key = UUID.randomUUID().toString();
        String code = lineCaptcha.getCode();
        String base64 = lineCaptcha.getImageBase64Data();
        return new CaptchaResult(key, code, base64);
    }
}
