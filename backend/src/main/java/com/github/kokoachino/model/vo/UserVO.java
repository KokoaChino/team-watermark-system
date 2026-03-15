package com.github.kokoachino.model.vo;

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

    @Schema(description = "角色：leader-队长，member-成员", example = "member")
    private String role;

    @Schema(description = "加入团队时间", example = "2026-02-10 14:30:00")
    private String joinedAt;
}
