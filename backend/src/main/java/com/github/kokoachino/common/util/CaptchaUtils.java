package com.github.kokoachino.common.util;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.github.kokoachino.model.vo.CaptchaVO;
import org.springframework.stereotype.Component;


/**
 * 验证码工具类
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Component
public class CaptchaUtils {

    public CaptchaVO createCaptcha() {
        // 定义图形验证码的长、宽、验证码字符数、干扰线宽度
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 10);
        return CaptchaVO.builder()
                .base64(lineCaptcha.getImageBase64Data())
                .code(lineCaptcha.getCode())
                .build();
    }
}
