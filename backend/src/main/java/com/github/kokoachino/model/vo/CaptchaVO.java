package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;


/**
 * 验证码 VO
 *
 * @author kokoachino
 * @date 2026-02-02
 */
@Data
@Builder
@Schema(description = "验证码信息")
public class CaptchaVO {

    @Schema(description = "验证码Key", example = "captcha:abc123")
    private String key;

    @Schema(description = "验证码图片Base64", example = "data:image/png;base64,iVBORw0KGgo...")
    private String base64;
}
