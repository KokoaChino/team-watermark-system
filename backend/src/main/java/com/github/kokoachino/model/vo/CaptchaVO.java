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

    @Schema(description = "验证码标识（用于验证）", example = "550e8400-e29b-41d4-a716-446655440000")
    private String key;
}