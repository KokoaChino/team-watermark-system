package com.github.kokoachino.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;


/**
 * Token 信息 VO
 *
 * @author kokoachino
 * @date 2026-02-03
 */
@Data
@Builder
@Schema(description = "Token 信息")
public class TokenVO {

    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "访问令牌有效期（秒）", example = "86400")
    private Long expiresIn;
}
