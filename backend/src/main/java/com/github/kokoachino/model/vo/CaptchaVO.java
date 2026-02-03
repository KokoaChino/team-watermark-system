package com.github.kokoachino.model.vo;

import lombok.Builder;
import lombok.Data;


/**
 * 验证码 VO
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Data
@Builder
public class CaptchaVO {

    private String base64;
    private String code;
    private String key;
}