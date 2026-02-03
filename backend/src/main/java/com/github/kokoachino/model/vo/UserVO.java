package com.github.kokoachino.model.vo;

import com.github.kokoachino.common.enums.TeamRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;


/**
 * 用户信息 VO
 *
 * @author kokoachino
 * @date 2026-01-31
 */
@Data
@Builder
@Schema(description = "用户信息")
public class UserVO {

    @Schema(description = "用户 ID", example = "1")
    private Integer id;

    @Schema(description = "用户名", example = "username")
    private String username;

    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "所属团队 ID", example = "1")
    private Integer teamId;

    /**
     * @see TeamRole
     */
    @Schema(description = "团队角色")
    private String teamRole;
}