package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "图形验证码")
public class CaptchaVO {

    @Schema(description = "Base64 编码的图片数据", example = "data:image/png;base64,iVBORw0KGgo...")
    private String base64;

    @Schema(description = "验证码标识（用于验证）", example = "f041f3e5-c8b4-4aa0-b59f-a764795310e7")
    private String key;
}